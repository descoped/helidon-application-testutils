package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.test.scenario.CaseFourTest;
import io.descoped.helidon.application.test.scenario.CaseMixedConfigurationTest;
import io.descoped.helidon.application.test.scenario.CaseNormalTest;
import io.descoped.helidon.application.test.scenario.CaseThreeTest;
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
                .testIdentifier(ClassOrMethodIdentifier.from(CaseNormalTest.class))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(CaseNormalTest.class, "createDeployment"))
                .build()
        );

        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(SuiteDeployment.class, "createDeployment"))
                .build()
        );

        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseThreeTest.class))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(CaseThreeTest.class, "createDeployment"))
                .build()
        );

        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseFourTest.class))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(CaseThreeTest.class, "createDeployment"))
                .build()
        );

        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideConfigurationIsCreated"))
                .deploymentIdentifier(ClassOrMethodIdentifier.from(CaseThreeTest.class, "createDeployment"))
                .build()
        );

        assertEquals(ClassOrMethodIdentifier.from(CaseNormalTest.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseNormalTest.class)).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseNormalTest.class)).testIdentifier.isClass());

        assertEquals(ClassOrMethodIdentifier.from(CaseNormalTest.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseNormalTest.class, "thatExtensionIsInvoked")).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseNormalTest.class, "thatExtensionIsInvoked")).testIdentifier.isClass()); // class level fallback deployment

        assertEquals(ClassOrMethodIdentifier.from(SuiteDeployment.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class)).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class)).testIdentifier.isClass());

        assertEquals(ClassOrMethodIdentifier.from(SuiteDeployment.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatExtensionIsInvoked")).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatExtensionIsInvoked")).testIdentifier.isClass());

        assertEquals(ClassOrMethodIdentifier.from(CaseThreeTest.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class)).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class)).testIdentifier.isClass());

        assertEquals(ClassOrMethodIdentifier.from(CaseThreeTest.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideConfigurationIsCreated")).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideConfigurationIsCreated")).testIdentifier.isMethod()); // method level deployment

        assertEquals(ClassOrMethodIdentifier.from(CaseThreeTest.class, "createDeployment"), deployments.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfigurationIsCreated")).deploymentIdentifier);
        assertTrue(deployments.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfigurationIsCreated")).testIdentifier.isClass()); // class level fallback deployment

        // CONFIGURATION
        // when class, register configuration profile
        // when method, register configuration override

        Configurations configurations = new Configurations();

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseNormalTest.class, "thatExtensionIsInvoked"))
                .build()
        );

        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseNormalTest.class, "thatExtensionIsInvoked"))
                .identicalTo(configurations.tryGet(ClassOrMethodIdentifier.from(CaseNormalTest.class, "thatExtensionIsInvoked"))));

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatExtensionIsInvoked"))
                .build()
        );

        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseNormalTest.class, "thatExtensionIsInvoked"))
                .identicalTo(configurations.tryGet(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatExtensionIsInvoked"))));

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatMethodOverrideConfigurationIsCreated"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "bar")))
                .build()
        );

        assertFalse(configurations.tryGet(ClassOrMethodIdentifier.from(CaseNormalTest.class, "thatExtensionIsInvoked"))
                .identicalTo(configurations.tryGet(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatMethodOverrideConfigurationIsCreated"))));

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatMethodOverrideProfileConfigurationIsCreated"))
                .configuration(new Configurations.ConfigurationProfile("dummy"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "bar")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatMethodOverrideProfileConfiguration2IsCreated"))
                .configuration(new Configurations.ConfigurationProfile("dummy"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "baz")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseThreeTest.class))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "bar")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideConfigurationIsCreated"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "bar")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfigurationIsCreated"))
                .configuration(new Configurations.ConfigurationProfile("dummy"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "bar")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfiguration2IsCreated"))
                .configuration(new Configurations.ConfigurationProfile("dummy"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "baz")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfiguration2IsCreated2"))
                .configuration(new Configurations.ConfigurationProfile("dummy"))
                .configuration(new Configurations.ConfigurationOverride(Map.of("foo", "baz")))
                .build()
        );

        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(ClassOrMethodIdentifier.from(CaseFourTest.class, "testServer"))
                .build()
        );

        assertPartialMap(Map.of(), configurations.tryGet(ClassOrMethodIdentifier.from(CaseNormalTest.class, "thatExtensionIsInvoked")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatExtensionIsInvoked")).testIdentifier.isClass());

        assertPartialMap(Map.of("foo", "bar"), configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatExtensionIsInvoked")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatExtensionIsInvoked")).testIdentifier.isClass());

        assertPartialMap(Map.of("foo", "bar"), configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideConfigurationIsCreated")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideConfigurationIsCreated")).testIdentifier.isMethod());

        assertPartialMap(Map.of("k1", "v1", "foo", "bar"), configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfigurationIsCreated")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfigurationIsCreated")).testIdentifier.isMethod());

        // method level configuration override (overrides ancestor at class level)
        assertPartialMap(Map.of("k1", "v1", "foo", "baz"), configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfiguration2IsCreated")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfiguration2IsCreated")).testIdentifier.isMethod());

        assertPartialMap(Map.of("foo", "bar"), configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "handleException")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "handleException")).testIdentifier.isClass());

        assertPartialMap(Map.of(), configurations.tryGet(ClassOrMethodIdentifier.from(CaseFourTest.class, "testServer")).toConfig().asMap().get());
        assertTrue(configurations.tryGet(ClassOrMethodIdentifier.from(CaseFourTest.class, "testServer")).testIdentifier.isMethod());


        // TEST METHOD REGISTRATION

        TestMethods testMethods = new TestMethods(deployments, configurations);

        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseNormalTest.class, "thatExtensionIsInvoked")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatExtensionIsInvoked")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatMethodOverrideConfigurationIsCreated")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatMethodOverrideProfileConfigurationIsCreated")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatExtensionIsInvoked")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideConfigurationIsCreated")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfigurationIsCreated")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfiguration2IsCreated")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfiguration2IsCreated2")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseThreeTest.class, "handleException")));
        testMethods.register(new TestMethod(ClassOrMethodIdentifier.from(CaseFourTest.class, "testServer")));

        // TEST METHOD COMPARISONS

        /*
            This comparison has high cognitive load as it implicitly compares deployment and configuration at method and fallback to class level against two test methods.

            Please read class and method level annotations between class and method scope and [Deployment|Configuration]Target comparators.
         */
        assertFalse(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatMethodOverrideConfigurationIsCreated"))
                .identicalTo(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseMixedConfigurationTest.class, "thatMethodOverrideProfileConfigurationIsCreated")))
        );

        assertTrue(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatExtensionIsInvoked"))
                .identicalTo(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideConfigurationIsCreated")))
        );

        assertTrue(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatExtensionIsInvoked"))
                .identicalTo(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "handleException")))
        );

        assertFalse(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatExtensionIsInvoked"))
                .identicalTo(testMethods.tryGet(ClassOrMethodIdentifier.from(CaseThreeTest.class, "thatMethodOverrideProfileConfigurationIsCreated")))
        );

        // TEST TEST METHOD

        TestMethod testMethodThatExtensionIsInvoked = testMethods.tryGet(ClassOrMethodIdentifier.from(CaseNormalTest.class, "thatExtensionIsInvoked"));
        assertEquals(ClassOrMethodIdentifier.from(CaseNormalTest.class, "createDeployment"), testMethodThatExtensionIsInvoked.deploymentTarget().deploymentIdentifier);
        assertTrue(testMethodThatExtensionIsInvoked.deploymentTarget().testIdentifier.isClass()); // class level fallback deployment
        assertPartialMap(Map.of(), testMethodThatExtensionIsInvoked.configurationTarget().toConfig().asMap().get());
        assertTrue(testMethodThatExtensionIsInvoked.configurationTarget().testIdentifier.isMethod());

        // EXECUTION PLAN

        ExecutionPlan executionPlan = new ExecutionPlan(testMethods);

        executionPlan.testTargetByContext(Context.ROOT).forEach((key, value) -> LOG.trace("ROOT: {}\n\t{}", key.deploymentIdentifier.className + "." + key.deploymentIdentifier.methodName, value.stream().map(m -> m.testIdentifier.className + "." + m.testIdentifier.methodName).collect(Collectors.joining("\n\t"))));
        executionPlan.testTargetByContext(Context.CLASS).forEach((key, value) -> LOG.trace("CLASS: {}\n\t{}", key.deploymentIdentifier.className + "." + key.deploymentIdentifier.methodName, value.stream().map(m -> m.testIdentifier.className + "." + m.testIdentifier.methodName).collect(Collectors.joining("\n\t"))));
        executionPlan.testTargetByContext(Context.METHOD).forEach((key, value) -> LOG.trace("METHOD: {}\n\t{}", key.deploymentIdentifier.className + "." + key.deploymentIdentifier.methodName, value.stream().map(m -> m.testIdentifier.className + "." + m.testIdentifier.methodName).collect(Collectors.joining("\n\t"))));


        // INIT TEST SERVER SUPPLIERS AND CREATE FACTORY

        TestServerFactory.createInitializer(TestServerFactory.Instance.TEST_PLAN).initialize(deployments, configurations, executionPlan);


        /*
            Question:
            - hva avgjør om en TestServerSupplier skal kjøres i scope av en: metode, klasse, eller suite?
            - hvordan skriver vi state til TestServerSupplier slik at den vet at den skal instansiere per metode, klasse, suite
            - bør du lage et TestServerLifecycle object som holder på slik state?

            Lazy:

            check if serverSupplier is already in Store, IF NOT find serverSupplier and write to Store
            start just in time

            stop: JUnit ClosableResource automatically close any resource during which Store.close is invoked (see JUnit Lifecycle)

            Extension:
            - beforeAll: handle SUITE and CLASS serverSupplier
            - beforeEach: find METHOD serverSupplier
            - beforeTestExecution: start and if method, stop
         */

    }


    static void assertPartialMap(Map<String, String> sourceMap, Map<String, String> containsMap) {
        // assert all source keyValue pairs
        sourceMap.forEach((key, value) -> {
            assertTrue(containsMap.containsKey(key), "Source map does not contain key: " + key + " in: " + sourceMap);
            assertEquals(containsMap.get(key), value);
        });
        // assert with containsMap keyValue pairs
        containsMap.entrySet()
                .stream()
                .filter(e -> !e.getKey().startsWith("webserver"))
                .forEach(e -> {
                    assertTrue(sourceMap.containsKey(e.getKey()), "Source map does not contain key: " + e.getKey() + " in: " + sourceMap);
                    assertEquals(sourceMap.get(e.getKey()), e.getValue());
                });
    }
}
