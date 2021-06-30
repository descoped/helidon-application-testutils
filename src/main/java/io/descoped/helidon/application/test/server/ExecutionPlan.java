package io.descoped.helidon.application.test.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

class ExecutionPlan {

    private final Map<ExecutionKey, List<TestMethod>> testServerTargetMap = new LinkedHashMap<>();

    ExecutionPlan(TestMethods testMethods) {
        Map<ExecutionKey, List<TestMethod>> classContextMap = new LinkedHashMap<>();
        Map<ExecutionKey, List<TestMethod>> methodContextMap = new LinkedHashMap<>();

        for (ClassOrMethodIdentifier testMethodIdentifier : testMethods.identifiers()) {
            TestMethod testMethod = testMethods.tryGet(testMethodIdentifier);
            ClassOrMethodIdentifier deploymentIdentifier = ofNullable(testMethod.deploymentTarget().deploymentIdentifier).orElseThrow();
            if (!deploymentIdentifier.isMethod()) {
                throw new IllegalStateException("The DeploymentIdentifier is missing the deployment method");
            }
            if (testMethod.context() == Context.CLASS) {
                // assumes that all test methods share same configuration. pick first.
                // if they are not same, they belongs to a different deployment instance.
                classContextMap.computeIfAbsent(
                        ExecutionKey.builder()
                                .deploymentIdentifier(deploymentIdentifier)
                                .context(testMethod.context())
                                .configurationTarget(testMethod.configurationTarget())
                                .build(),
                        ignore -> new ArrayList<>()).add(testMethod);
            } else if (testMethod.context() == Context.METHOD) {
                // assumes that all test methods share same configuration. pick first.
                // if they are not same, they belongs to a different deployment instance.
                methodContextMap.computeIfAbsent(
                        ExecutionKey.builder()
                                .deploymentIdentifier(deploymentIdentifier)
                                .context(testMethod.context())
                                .configurationTarget(testMethod.configurationTarget())
                                .build(),
                        ignore -> new ArrayList<>()).add(testMethod);
            } else {
                throw new UnsupportedOperationException();
            }
        }

        /*
            Please note: an ExecutionKey is unique given that [DeploymentTarget + ConfigurationTarget + Context] are equal.
            This provides a guarantee that each test method represented by an ExecutionKey can share the same execution environment.

            Resolution:
            - Context.[ROOT|CLASS]: MAY contain the same TestClass in both scopes
            - Context.[METHOD]:     CAN only contain a single TestMethod
         */

        for (Map.Entry<ExecutionKey, List<TestMethod>> entry : classContextMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                // if we got multiple test methods that share the same ExecutionKey, it is elected to run in JUnit Container Root context
                testServerTargetMap.computeIfAbsent(entry.getKey().copyAndUpdateContext(Context.ROOT), ignore -> new ArrayList<>()).addAll(entry.getValue());
            } else {
                // if we got a single test method that share the same ExecutionKey, it is elected to run in JUnit Container Class context
                testServerTargetMap.computeIfAbsent(entry.getKey().copyAndUpdateContext(Context.CLASS), ignore -> new ArrayList<>()).addAll(entry.getValue());
            }
        }

        for (Map.Entry<ExecutionKey, List<TestMethod>> entry : methodContextMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                // if we got multiple test methods that share the same ExecutionKey, it is elected to run in JUnit Container Class context
                testServerTargetMap.computeIfAbsent(entry.getKey().copyAndUpdateContext(Context.CLASS), ignore -> new ArrayList<>()).addAll(entry.getValue());
            } else {
                // if we got a single test method that share the same ExecutionKey, it is elected to run in JUnit Container Method context
                testServerTargetMap.computeIfAbsent(entry.getKey().copyAndUpdateContext(Context.METHOD), ignore -> new ArrayList<>()).addAll(entry.getValue());
            }
        }
    }

    Map<ExecutionKey, List<TestMethod>> allTestTargets() {
        return testServerTargetMap;
    }

    Map<ExecutionKey, List<TestMethod>> testTargetByContext(Context context) {
        return testServerTargetMap.entrySet()
                .stream()
                .filter(p -> p.getKey().context.equals(context))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, u) -> o, LinkedHashMap::new));
    }
}
