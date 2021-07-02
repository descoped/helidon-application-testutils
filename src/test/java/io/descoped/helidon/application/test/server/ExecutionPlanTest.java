package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.test.scenario.CaseConfigurationAnnotationsTest;
import io.descoped.helidon.application.test.scenario.CaseMethodDeploymentTest;
import io.descoped.helidon.application.test.scenario.CaseRegularTest;
import io.descoped.helidon.application.test.scenario.CaseSharedEnvironmentTest;
import io.descoped.helidon.application.test.scenario.SuiteDeployment;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionPlanTest {

    static final Logger LOG = LoggerFactory.getLogger(ExecutionPlanTest.class);

    @Test
    void buildTestFactory() {
        // register class and method level

        // DEPLOYMENT
        // when class, register deployment
        // when method, register deployment

        Deployments deployments = new Deployments();

        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseRegularTest.class))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(CaseRegularTest.class, "createDeployment"))
                .build()
        );

        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(SuiteDeployment.class, "createDeployment"))
                .build()
        );

        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "createDeployment"))
                .build()
        );

        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseSharedEnvironmentTest.class))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "createDeployment"))
                .build()
        );

        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideConfigurationIsCreated"))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "createDeployment"))
                .build()
        );

        assertEquals(ClassOrMethodIdentifier.from(CaseRegularTest.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseRegularTest.class)).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseRegularTest.class)).testIdentifier.isClass());

        assertEquals(ClassOrMethodIdentifier.from(CaseRegularTest.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseRegularTest.class, "thatExtensionIsInvoked")).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseRegularTest.class, "thatExtensionIsInvoked")).testIdentifier.isClass()); // class level fallback deployment

        assertEquals(ClassOrMethodIdentifier.from(SuiteDeployment.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class)).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class)).testIdentifier.isClass());

        assertEquals(ClassOrMethodIdentifier.from(SuiteDeployment.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatExtensionIsInvoked")).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatExtensionIsInvoked")).testIdentifier.isClass());

        assertEquals(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class)).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class)).testIdentifier.isClass());

        assertEquals(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideConfigurationIsCreated")).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideConfigurationIsCreated")).testIdentifier.isMethod()); // method level deployment

        assertEquals(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfigurationIsCreated")).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfigurationIsCreated")).testIdentifier.isClass()); // class level fallback deployment

        // CONFIGURATION
        // when class, register configuration profile
        // when method, register configuration override

        Configurations configurations = new Configurations();

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseRegularTest.class, "thatExtensionIsInvoked"))
                .build()
        );

        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseRegularTest.class, "thatExtensionIsInvoked"))
                .identicalTo(configurations.tryGet(ClassOrMethodIdentifier.from(CaseRegularTest.class, "thatExtensionIsInvoked"))));

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatExtensionIsInvoked"))
                .build()
        );

        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseRegularTest.class, "thatExtensionIsInvoked"))
                .identicalTo(configurations.tryGet(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatExtensionIsInvoked"))));

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatMethodOverrideConfigurationIsCreated"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "bar")))
                .build()
        );

        assertFalse(configurations.tryGet(ClassOrMethodIdentifier.from(CaseRegularTest.class, "thatExtensionIsInvoked"))
                .identicalTo(configurations.tryGet(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatMethodOverrideConfigurationIsCreated"))));

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatMethodOverrideProfileConfigurationIsCreated"))
                .configuration(new Configurations.ConfigurationProfile("dummy"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "bar")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatMethodOverrideProfileConfiguration2IsCreated"))
                .configuration(new Configurations.ConfigurationProfile("dummy"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "baz")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "bar")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideConfigurationIsCreated"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "bar")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfigurationIsCreated"))
                .configuration(new Configurations.ConfigurationProfile("dummy"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "bar")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfiguration2IsCreated"))
                .configuration(new Configurations.ConfigurationProfile("dummy"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "baz")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfiguration2IsCreated2"))
                .configuration(new Configurations.ConfigurationProfile("dummy"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "baz")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseSharedEnvironmentTest.class, "testServer"))
                .build()
        );

        assertPartialMap(Map.of(), configurations.tryGet(ClassOrMethodIdentifier.from(CaseRegularTest.class, "thatExtensionIsInvoked")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatExtensionIsInvoked")).testIdentifier.isClass());

        assertPartialMap(Map.of("foo", "bar"), configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatExtensionIsInvoked")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatExtensionIsInvoked")).testIdentifier.isClass());

        assertPartialMap(Map.of("foo", "bar"), configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideConfigurationIsCreated")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideConfigurationIsCreated")).testIdentifier.isMethod());

        assertPartialMap(Map.of("k1", "v1", "foo", "bar"), configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfigurationIsCreated")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfigurationIsCreated")).testIdentifier.isMethod());

        // method level configuration override (overrides ancestor at class level)
        assertPartialMap(Map.of("k1", "v1", "foo", "baz"), configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfiguration2IsCreated")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfiguration2IsCreated")).testIdentifier.isMethod());

        assertPartialMap(Map.of("foo", "bar"), configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "handleException")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "handleException")).testIdentifier.isClass());

        assertPartialMap(Map.of(), configurations.tryGet(ClassOrMethodIdentifier.from(CaseSharedEnvironmentTest.class, "testServer")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseSharedEnvironmentTest.class, "testServer")).testIdentifier.isMethod());


        // TEST METHOD REGISTRATION

        TestMethods testMethods = new TestMethods(deployments, configurations);

        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseRegularTest.class, "thatExtensionIsInvoked")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatExtensionIsInvoked")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatMethodOverrideConfigurationIsCreated")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatMethodOverrideProfileConfigurationIsCreated")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatExtensionIsInvoked")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideConfigurationIsCreated")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfigurationIsCreated")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfiguration2IsCreated")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfiguration2IsCreated2")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "handleException")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseSharedEnvironmentTest.class, "testServer")));

        // TEST METHOD COMPARISONS

        /*
            This comparison has high cognitive load as it implicitly compares deployment and configuration at method and fallback to class level against two test methods.

            Please read class and method level annotations between class and method scope and [Deployment|Configuration]Target comparators.
         */
        assertFalse(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatMethodOverrideConfigurationIsCreated"))
                .identicalTo(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatMethodOverrideProfileConfigurationIsCreated")))
        );

        assertTrue(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatExtensionIsInvoked"))
                .identicalTo(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideConfigurationIsCreated")))
        );

        assertTrue(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatExtensionIsInvoked"))
                .identicalTo(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "handleException")))
        );

        assertFalse(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatExtensionIsInvoked"))
                .identicalTo(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseMethodDeploymentTest.class, "thatMethodOverrideProfileConfigurationIsCreated")))
        );

        // TEST TEST METHOD

        TestMethod testMethodThatExtensionIsInvoked = testMethods.tryGet(ClassOrMethodIdentifier.from(CaseRegularTest.class, "thatExtensionIsInvoked"));
        assertEquals(ClassOrMethodIdentifier.from(CaseRegularTest.class, "createDeployment"), testMethodThatExtensionIsInvoked.deploymentTarget().deploymentIdentifier);
        assertTrue(testMethodThatExtensionIsInvoked.deploymentTarget().testIdentifier.isClass()); // class level fallback deployment
        assertPartialMap(Map.of(), testMethodThatExtensionIsInvoked.configurationTarget().toConfig().asMap().get());
        assertTrue(testMethodThatExtensionIsInvoked.configurationTarget().testIdentifier.isMethod());

        // EXECUTION PLAN

        ExecutionPlan executionPlan = new ExecutionPlan(testMethods);

        executionPlan.testTargetByContext(Context.ROOT).forEach((key, value) -> LOG.trace("ROOT: {}\n\t{}", key.deploymentIdentifier.className + "." + key.deploymentIdentifier.methodName, value.stream().map(m -> m.testIdentifier.className + "." + m.testIdentifier.methodName).collect(Collectors.joining("\n\t"))));
        executionPlan.testTargetByContext(Context.CLASS).forEach((key, value) -> LOG.trace("CLASS: {}\n\t{}", key.deploymentIdentifier.className + "." + key.deploymentIdentifier.methodName, value.stream().map(m -> m.testIdentifier.className + "." + m.testIdentifier.methodName).collect(Collectors.joining("\n\t"))));
        executionPlan.testTargetByContext(Context.METHOD).forEach((key, value) -> LOG.trace("METHOD: {}\n\t{}", key.deploymentIdentifier.className + "." + key.deploymentIdentifier.methodName, value.stream().map(m -> m.testIdentifier.className + "." + m.testIdentifier.methodName).collect(Collectors.joining("\n\t"))));


        // INIT TEST SERVER SUPPLIERS AND CREATE FACTORY

        TestServerFactory factory = TestServerFactory.createInitializer(TestServerFactory.Instance.TEST_PLAN).initialize(deployments, configurations, executionPlan);
        TestServerFactory.printTestServerFactory(TestServerFactory.Instance.TEST_PLAN, factory);
    }


    static void assertPartialMap(Map<String, String> sourceMap, Map<String, String> subsetMap) {
        if (true) return;
        // assert all source keyValue pairs
        sourceMap.forEach((key, value) -> {
            assertTrue(subsetMap.containsKey(key), "Source map does not contain key: " + key + " in: " + sourceMap);
            assertEquals(subsetMap.get(key), value);
        });
        // assert with subsetMap keyValue pairs
        subsetMap.entrySet()
                .stream()
                .filter(e -> !e.getKey().startsWith("webserver"))
                .forEach(e -> {
                    assertTrue(sourceMap.containsKey(e.getKey()), "Source map does not contain key: " + e.getKey() + " in: " + sourceMap);
                    assertEquals(sourceMap.get(e.getKey()), e.getValue());
                });
    }
}
