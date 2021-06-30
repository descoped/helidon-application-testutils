package io.descoped.helidon.application.test.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class TestServerExecutionListener implements TestExecutionListener {

    static final Logger LOG = LoggerFactory.getLogger(TestServerExecutionListener.class);

    private final TestServerFactory.Instance testServerFactoryInstance;

    // no param constructor is required by JUnit Launcher engine
    @SuppressWarnings("unused")
    public TestServerExecutionListener() {
        testServerFactoryInstance = TestServerFactory.Instance.DEFAULT;
    }

    // package private constructor used in listener tests
    TestServerExecutionListener(TestServerFactory.Instance instance) {
        testServerFactoryInstance = instance;
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if (testPlan.getRoots().size() > 1) {
            throw new RuntimeException("The TestServerExtension is limited to a single test plan!");
        }

        TestIdentifier rootIdentifier = testPlan.getRoots().iterator().next();

        final Deployments deployments = new Deployments();
        final Configurations configurations = new Configurations();
        final TestMethods testMethods = new TestMethods(deployments, configurations);

        // traverse test plan identifiers
        walk(testPlan, rootIdentifier, (ancestors, testIdentifier) -> {
            switch (Context.ordinalOf(ancestors.size())) {
                case ROOT:
                    // ignore
                    break;
                case CLASS:
                    dealWithClassContext(deployments, configurations, testIdentifier);
                    break;
                case METHOD:
                    dealWithMethodContext(deployments, configurations, testMethods, testIdentifier);
                    break;
                default:
                    throw new IllegalStateException();
            }
        });

        if (testMethods.identifiers().isEmpty()) {
            return;
        }

        TestServerFactory factory = TestServerFactory.createInitializer(testServerFactoryInstance).initialize(deployments, configurations, new ExecutionPlan(testMethods));

        if (TestServerFactory.Instance.DEFAULT == testServerFactoryInstance) {
            printTestServerFactory(testServerFactoryInstance, factory);
        }
    }

    private void dealWithClassContext(Deployments deployments, Configurations configurations, TestIdentifier testIdentifier) {
        ClassSource classSource = (ClassSource) testIdentifier.getSource().orElseThrow();
        Class<?> testClass = classSource.getJavaClass();

        if (!isExtendWithPresent(testClass)) {
            return;
        }

        // class level deployment
        Class<?> deploymentTargetClass;
        Method deploymentTargetMethod;
        if (testClass.isAnnotationPresent(Deployment.class)) {
            deploymentTargetClass = testClass.getDeclaredAnnotation(Deployment.class).target();
            if (deploymentTargetClass == Void.class) {
                throw new IllegalStateException("The @Deployment annotation requires 'target' property to be set at class level! " + testClass);
            }
            deploymentTargetMethod = AnnotationUtils.findAnnotatedMethods(deploymentTargetClass, Deployment.class, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                    .stream()
                    .filter(method -> !method.isAnnotationPresent(Test.class))
                    .reduce((u, v) -> {
                        throw new IllegalStateException("More than one @Deployment found!");
                    })
                    .orElseThrow(() -> new IllegalStateException("Missing deployment for class '" + classSource.getClassName() + "'!"));
        } else {
            deploymentTargetClass = testClass;
            deploymentTargetMethod = AnnotationUtils.findAnnotatedMethods(testClass, Deployment.class, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                    .stream()
                    .filter(method -> !method.isAnnotationPresent(Test.class))
                    .reduce((u, v) -> {
                        throw new IllegalStateException("More than one @Deployment found!");
                    })
                    .orElseThrow(() -> new IllegalStateException("Missing deployment for class '" + classSource.getClassName() + "'!"));
        }
        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(classSource))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(deploymentTargetClass, deploymentTargetMethod))
                .build());

        // class level configuration
        ConfigurationTarget.Builder builder = new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(classSource));
        if (List.of(ConfigurationProfile.class, ConfigurationOverride.class).stream().anyMatch(testClass::isAnnotationPresent)) {

            if (testClass.isAnnotationPresent(ConfigurationProfile.class)) {
                List.of(testClass.getDeclaredAnnotationsByType(ConfigurationProfile.class)).stream()
                        .map(anno -> new Configurations.ConfigurationProfile(anno.value()))
                        .forEach(builder::configuration);
            }

            if (testClass.isAnnotationPresent(ConfigurationOverride.class)) {
                List.of(testClass.getDeclaredAnnotationsByType(ConfigurationOverride.class)).stream()
                        .map(anno -> {
                            Map<String, String> configurationOverrideMap = new LinkedHashMap<>();
                            String[] keyAndValuePairs = anno.value();
                            convertOverrideKeyValuePairsToMap(keyAndValuePairs, configurationOverrideMap, classSource);
                            return new Configurations.ConfigurationOverride(configurationOverrideMap);
                        })
                        .forEach(builder::configuration);
            }

        }
        configurations.register(builder.build());
    }

    private void dealWithMethodContext(Deployments deployments, Configurations configurations, TestMethods testMethods, TestIdentifier testIdentifier) {
        MethodSource methodSource = (MethodSource) testIdentifier.getSource().orElseThrow();
        Class<?> testClass = methodSource.getJavaClass();

        if (!isExtendWithPresent(testClass)) {
            return;
        }

        List<Method> matchingMethods = findMethodsByName(testClass, methodSource);

        for (Method testMethod : matchingMethods) {
            // method level deployment
            if (testMethod.isAnnotationPresent(Deployment.class)) { //  && !testMethod.isAnnotationPresent(Test.class)
                Class<?> deploymentTargetClass = testMethod.getDeclaredAnnotation(Deployment.class).target();
                if (deploymentTargetClass == Void.class) {
                    throw new IllegalStateException("The @Deployment annotation requires 'target' property to be set at method level!");
                }
                Method deploymentTargetMethod = AnnotationUtils.findAnnotatedMethods(deploymentTargetClass, Deployment.class, ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                        .stream()
                        .filter(method -> !method.isAnnotationPresent(Test.class))
                        .reduce((u, v) -> {
                            throw new IllegalStateException("More than one @Deployment found!");
                        })
                        .orElseThrow(() -> new IllegalStateException("Missing deployment for class '" + deploymentTargetClass + "'!"));
                deployments.register(new DeploymentTarget.Builder()
                        .testIdentifier(ClassOrMethodIdentifier.from(methodSource))
                        .deploymentIdentifier(ClassOrMethodIdentifier.from(deploymentTargetClass, deploymentTargetMethod))
                        .build());
            }

            // method level configuration
            if (List.of(ConfigurationProfile.class, ConfigurationOverride.class).stream().anyMatch(testMethod::isAnnotationPresent)) {
                ConfigurationTarget.Builder builder = new ConfigurationTarget.Builder()
                        .testIdentifier(ClassOrMethodIdentifier.from(methodSource));

                if (testMethod.isAnnotationPresent(ConfigurationProfile.class)) {
                    List.of(testMethod.getDeclaredAnnotationsByType(ConfigurationProfile.class)).stream()
                            .map(anno -> new Configurations.ConfigurationProfile(anno.value()))
                            .forEach(builder::configuration);
                }

                if (testMethod.isAnnotationPresent(ConfigurationOverride.class)) {
                    List.of(testMethod.getDeclaredAnnotationsByType(ConfigurationOverride.class)).stream()
                            .map(anno -> {
                                Map<String, String> configurationOverrideMap = new LinkedHashMap<>();
                                String[] keyAndValuePairs = anno.value();
                                convertOverrideKeyValuePairsToMap(keyAndValuePairs, configurationOverrideMap, methodSource);
                                return new Configurations.ConfigurationOverride(configurationOverrideMap);
                            })
                            .forEach(builder::configuration);
                }

                configurations.register(builder.build());
            }

            // register test method
            testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(methodSource)));
        }
    }


    private boolean isExtendWithPresent(Class<?> javaClass) {
        return ofNullable(javaClass).orElseThrow().isAnnotationPresent(ExtendWith.class) &&
                List.of(javaClass.getAnnotation(ExtendWith.class).value()).stream().anyMatch(p -> p == TestServerExtension.class);
    }

    void convertOverrideKeyValuePairsToMap(String[] sourceArray, Map<String, String> targetMap, TestSource testSource) {
        if (sourceArray.length % 2 != 0) {
            throw new IllegalArgumentException(String.format("Wrong number of arguments (%s) for @ConfigurationOverride at %s",
                    sourceArray.length, testSource.toString()));
        }
        for (int i = 0; i < sourceArray.length; i += 2) {
            targetMap.put(sourceArray[i], sourceArray[i + 1]);
        }
    }

    List<Method> findMethodsByName(Class<?> javaClass, MethodSource methodSource) {
        List<Method> matchingMethods = ReflectionSupport.findMethods(javaClass, method -> methodSource.getMethodName().equals(method.getName()), HierarchyTraversalMode.TOP_DOWN);
        if (matchingMethods.isEmpty()) {
            throw new IllegalStateException("Unable to find method: " + methodSource.getMethodName() + " in class: " + javaClass.getName());
        }
        return matchingMethods;
    }

    private void walk(TestPlan testPlan, TestIdentifier currentTestIdentifier, BiConsumer<Set<TestIdentifier>, TestIdentifier> visitor) {
        traverse(0, new LinkedHashSet<>(), testPlan, new LinkedHashSet<>(), currentTestIdentifier, visitor);
    }

    private void traverse(int depth,
                          Set<TestIdentifier> visitedTestIdentifier,
                          TestPlan testPlan, Set<TestIdentifier> ancestors,
                          TestIdentifier currentTestIdentifier,
                          BiConsumer<Set<TestIdentifier>, TestIdentifier> visitor) {

        if (!visitedTestIdentifier.add(currentTestIdentifier)) {
            return;
        }

        visitor.accept(ancestors, currentTestIdentifier);

        ancestors.add(currentTestIdentifier);

        Set<TestIdentifier> children = testPlan.getChildren(currentTestIdentifier);
        try {
            for (TestIdentifier child : children) {
                traverse(depth + 1, visitedTestIdentifier, testPlan, ancestors, child, visitor);
            }
        } finally {
            ancestors.remove(currentTestIdentifier);
        }
    }

    static void printTestServerFactory(TestServerFactory.Instance testServerFactoryInstance, TestServerFactory testServerFactory) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("\nExecution Tree ({}):\n\n\t{}", testServerFactoryInstance, testServerFactory.keySet()
                    .stream()
                    .sorted(Comparator.comparingInt(cmp -> cmp.executionKey.context.ordinal()))
                    .map(builder -> "[" + builder.executionKey.context + "] @ " + builder.executionKey.deploymentIdentifier.className + "." + builder.executionKey.deploymentIdentifier.methodName + "\n\t|\n\t| " +
                            builder.testMethods.stream()
                                    .map(m -> m.testIdentifier.className + "." + m.testIdentifier.methodName)
                                    .collect(Collectors.joining("\n\t| ")))
                    .map(m -> m + "\n\t")
                    .collect(Collectors.joining("\n\t")));
        }
    }
}
