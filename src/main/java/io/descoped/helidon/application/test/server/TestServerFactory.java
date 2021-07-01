package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.base.HelidonDeployment;
import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.utils.StackTraceUtils;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class TestServerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TestServerFactory.class);

    /*
     * The TestServerExtension maintains current execution test server target in beforeTestExecution
     */
    final Deployments deployments;
    final Configurations configurations;
    private final AtomicReference<TestServerExtension.TestServerResource> currentTestServerResourceReference = new AtomicReference<>();
    private final Map<TestServerIdentifier, TestServerSupplier> supplierByContextMap;

    private TestServerFactory(Deployments deployments, Configurations configurations, Map<TestServerIdentifier, TestServerSupplier> supplierByContextMap) {
        this.deployments = deployments;
        this.configurations = configurations;
        this.supplierByContextMap = supplierByContextMap;
    }

    enum Instance {
        DEFAULT,
        TEST_LISTENER,
        TEST_PLAN,
        TEST_SERVER_FACTORY
    }

    static class TestServerFactorySingleton {
        private static final AtomicReference<Map<Instance, TestServerFactory>> instanceRef = new AtomicReference<>(new ConcurrentHashMap<>());

        private static TestServerFactory get() {
            return instanceRef.get().get(Instance.DEFAULT);
        }

        private static TestServerFactory get(Instance instance) {
            return instanceRef.get().get(instance);
        }

        private static void set(Instance instance, TestServerFactory factory) {
            //LOG.trace("SET FACTORY: {}\n{}", factory, StackTraceUtils.printStackTrace());
            if (instanceRef.get().putIfAbsent(instance, factory) != null) {
                throw new IllegalStateException("Illegal attempt to mutate singleton factory: " + instance);
            }
        }
    }

    public static TestServerFactory instance() {
        return TestServerFactorySingleton.get();
    }

    static TestServerFactory instance(Instance name) {
        return TestServerFactorySingleton.get(name);
    }

    public Set<TestServerIdentifier> keySet() {
        return supplierByContextMap.keySet();
    }

    public TestServerSupplier get(TestServerIdentifier testServerIdentifier) {
        return supplierByContextMap.get(testServerIdentifier);
    }

    public Map<TestServerIdentifier, TestServerSupplier> findTestServerSuppliersForRootOrClassContextByClassIdentifier(ClassOrMethodIdentifier testIdentifier) {
        return supplierByContextMap.entrySet().stream()
                .filter(e -> e.getKey().executionKey.context != Context.METHOD && e.getKey().testMethods.stream().anyMatch(m -> m.testIdentifier.className.equals(testIdentifier.className)))
                .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(cmp -> cmp.executionKey.context.ordinal())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, u) -> o, LinkedHashMap::new));
    }

    public Map.Entry<TestServerIdentifier, TestServerSupplier> findTestServerSuppliersForMethodContextByMethodIdentifier(ClassOrMethodIdentifier testMethodIdentifier) {
        return supplierByContextMap.entrySet().stream()
                .filter(e -> e.getKey().testMethods.stream().anyMatch(testMethod -> testMethod.testIdentifier.equals(testMethodIdentifier)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Supplier NOT found for: " + testMethodIdentifier + "\n" + StackTraceUtils.printStackTrace()));
    }

    public TestServerSupplier findByTestClassIdentifierAndContext(ClassOrMethodIdentifier testIdentifier, Context context) {
        return supplierByContextMap.entrySet().stream()
                .filter(e -> e.getKey().executionKey.context == context && e.getKey().testMethods.stream().anyMatch(m -> m.testIdentifier.className.equals(testIdentifier.className)))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    /**
     * see field comment
     *
     * @param testServerResource
     */
    void setCurrentTestServerResource(TestServerExtension.TestServerResource testServerResource) {
        currentTestServerResourceReference.set(testServerResource);
    }

    /**
     * Returns the current server depending on scope.
     *
     * @return @BeforeAll a root scoped instance will always be returned. <br/> @BeforeEach a class or root scoped instance will be returned
     */
    public TestServer currentServer() {
        TestServerExtension.TestServerResource testServerResource = currentTestServerResourceReference.get();
        if (testServerResource == null) {
            throw new IllegalStateException("The test server has yet NOT been created!");
        }
        return testServerResource.getServer();
    }

    /**
     * Returns the current client depending on scope.
     *
     * @return @BeforeAll a root scoped client will always be returned. <br/> @BeforeEach a class or root scoped client will be returned
     */
    public TestClient currentClient() {
        TestServerExtension.TestServerResource testServerResource = currentTestServerResourceReference.get();
        if (testServerResource == null) {
            throw new IllegalStateException("The test server has yet NOT been created!");
        }
        return testServerResource.getClient();
    }

    static Initializer createInitializer(Instance name) {
        return new Initializer(name);
    }

    static class Initializer {
        private final Instance instance;
        private final Map<TestServerIdentifier, TestServerSupplier> supplierByContextMap = new LinkedHashMap<>();

        public Initializer(Instance instance) {
            this.instance = instance;
        }

        TestServerFactory initialize(Deployments deployments, Configurations configurations, ExecutionPlan plan) {
            // iterate suite scope
            buildSuppliers(plan.allTestTargets(), supplierByContextMap);

            // initialize factory
            TestServerFactory factory = new TestServerFactory(deployments, configurations, supplierByContextMap);
            TestServerFactorySingleton.set(instance, factory);
            return factory;
        }

        private void buildSuppliers(Map<ExecutionKey, List<TestMethod>> source, Map<TestServerIdentifier, TestServerSupplier> target) {
            for (Map.Entry<ExecutionKey, List<TestMethod>> entry : source.entrySet()) {
                ExecutionKey executionKey = entry.getKey(); // deployment and scope
                List<TestMethod> testMethodList = entry.getValue(); // test methods that belongs to key

                // get deployment
                Class<?> clazz = ReflectionUtils.tryToLoadClass(executionKey.deploymentIdentifier.className).getOrThrow(this::handleException);
                Method method = ReflectionUtils.findMethod(clazz, ofNullable(executionKey.deploymentIdentifier.methodName).orElseThrow()).orElseThrow();
                HelidonDeployment helidonDeployment = ofNullable(ReflectionUtils.invokeMethod(method, null))
                        .map(HelidonDeployment.class::cast)
                        .orElseThrow(() -> new IllegalStateException("No deployment found"));

                // merge factory config with user provided defaultConfig
                HelidonDeployment.Builder helidonDeploymentBuilder = HelidonDeployment.of(helidonDeployment);
                Config.Builder deploymentTargetConfigBuilder = helidonDeploymentBuilder.configBuilder();
                Config.Builder testTargetConfigBuilder = executionKey.configurationTarget.toConfigBuilder();
                testTargetConfigBuilder.addSource(ConfigSources.create(deploymentTargetConfigBuilder.build()));
                helidonDeploymentBuilder.configBuilder(testTargetConfigBuilder);

                // create supplier
                TestServerSupplier testServerSupplier = TestServerSupplier.create(helidonDeploymentBuilder);
                TestServerIdentifier testServerIdentifier = new TestServerIdentifier(executionKey, testMethodList);
                target.put(testServerIdentifier, testServerSupplier);
            }
        }

        RuntimeException handleException(Exception e) {
            if (e instanceof RuntimeException) {
                return (RuntimeException) e;
            }
            return new LoadTestClassException(e);
        }
    }
}
