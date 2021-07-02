package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.test.scenario.CaseConfigurationAnnotationsTest;
import io.descoped.helidon.application.test.scenario.CaseMethodDeploymentTest;
import io.descoped.helidon.application.test.scenario.CaseRegularTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.ClassNameFilter.excludeClassNamePatterns;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

class TestServerExecutionListenerTest {

    static final Logger LOG;

    static {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LOG = LoggerFactory.getLogger(TestServerExecutionListenerTest.class);
    }

    @Test
    void thatConfigurationFactoryDiscoversAllTestConfigurations() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectPackage(CaseRegularTest.class.getPackageName()),
                        selectClass(CaseRegularTest.class),
                        selectClass(CaseConfigurationAnnotationsTest.class),
                        selectClass(CaseMethodDeploymentTest.class)
                )
                .filters(
                        includeClassNamePatterns(".*Case(\\w+)Test$"),
                        excludeClassNamePatterns("." + TestServerExecutionListenerTest.class.getSimpleName())
                )
                .build();

        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);

        /*
         * Invocation of TestServerExecutionListener.testPlanExecutionStarted triggers the ConfigurationFactory,
         * DeploymentFactory and TestServerFactory to be initialized.
         */

        TestServerExecutionListener testServerExecutionListener = new TestServerExecutionListener(TestServerFactory.Instance.TEST_LISTENER);

        testServerExecutionListener.testPlanExecutionStarted(testPlan);

        final TestServerFactory testServerFactory = TestServerFactory.instance(TestServerFactory.Instance.TEST_LISTENER);

        assertNotNull(testServerFactory);

        ClassOrMethodIdentifier methodOverrideProfile = ClassOrMethodIdentifier.from(CaseConfigurationAnnotationsTest.class, "thatMethodOverrideConfigurationIsCreated");
        TestServerSupplier supplier = testServerFactory.findByTestClassIdentifierAndContext(methodOverrideProfile, Context.CLASS);

        assertNotNull(supplier);

        Map<String, String> methodOverrideProfileMap = supplier.get().config().asMap().get();
        assertEquals("bar", methodOverrideProfileMap.get("foo"));

        //printConfiguration(testServerExecutionListener.configurations);
    }

    @Test
    void testConfigurationOverrideKeyValuePairs() {
        assertThrows(IllegalArgumentException.class, () -> {
            String[] configurationOverrideValues = {"k1", "v1", "k2"};
            new TestServerExecutionListener(TestServerFactory.Instance.TEST_LISTENER)
                    .convertOverrideKeyValuePairsToMap(configurationOverrideValues, new LinkedHashMap<>(), MethodSource.from("Foo", "method"));
        });
    }

    @Test
    void thatReflectionFindMethodsByName() {
        TestServerExecutionListener listener = new TestServerExecutionListener(TestServerFactory.Instance.TEST_LISTENER);
        List<Method> methods = listener.findMethodsByName(CaseRegularTest.class, MethodSource.from(CaseRegularTest.class.getName(), "thatExtensionIsInvoked"));
        assertEquals(1, methods.size());

        assertThrows(IllegalStateException.class, () -> listener.findMethodsByName(CaseRegularTest.class, MethodSource.from(CaseRegularTest.class.getName(), "methodDoesNotExist")));
    }

    private void printConfiguration(Configurations factory) {
        Set<ClassOrMethodIdentifier> ClassOrMethodIdentifiers = factory.keySet();
        for (ClassOrMethodIdentifier ClassOrMethodIdentifier : ClassOrMethodIdentifiers) {
            LOG.trace("configurationKey: {} -> \n\t{}",
                    ClassOrMethodIdentifier,
                    factory.tryGet(ClassOrMethodIdentifier).toConfig().asMap().get()
                            .entrySet()
                            .stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .collect(Collectors.joining("\n\t,"))
            );
        }
    }
}
