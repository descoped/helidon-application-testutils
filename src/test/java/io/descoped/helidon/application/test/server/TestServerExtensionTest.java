package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.scenario.SuiteDeployment;
import io.helidon.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TestServerExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Deployment(target = SuiteDeployment.class)
class TestServerExtensionTest {

    @Inject
    TestClient testClient;

    @Inject
    TestServer testServer;

    @Test
    void testDefaultMethodConfiguration() {
        Config defaultConfiguration = testServer.config();
        assertNotNull(defaultConfiguration);
        assertFalse(defaultConfiguration.get("k1").exists(), "Key 'k1' exists!");
        assertConfiguration("testDefaultMethodConfiguration");
    }

    @ConfigurationOverride({"k1", "v1"})
    @Test
    void testMethodConfigurationOverride1() throws Exception {
        assertNotNull(testClient);
        assertNotNull(testServer);
        assertConfiguration("testMethodConfigurationOverride1", "k1", "v1");
    }

    @ConfigurationOverride({"k2", "v2"})
    @Test
    void testMethodConfigurationOverride2() throws Exception {
        assertNotNull(testClient);
        assertNotNull(testServer);
        assertConfiguration("testMethodConfigurationOverride2", "k2", "v2");
    }

    @Test
    void testTestServerAndClientParameters(TestServer server, TestClient client) {
        assertEquals(testServer, server);
        assertEquals(testClient, client);
    }

    private void assertConfiguration(String testMethodName, String... keyAndValuePairs) {
        Config testMethodConfiguration = TestServerFactory.instance().configurations.tryGet(ClassOrMethodIdentifier.from(TestServerExtensionTest.class.getName(), testMethodName)).toConfig();
        assertNotNull(testMethodConfiguration);
        List<String> keyAndValueList = List.of(keyAndValuePairs);
        assertEquals(0, keyAndValueList.size() % 2, "KeyValue pairs are not even!");
        IntStream.range(0, keyAndValueList.size())
                .filter(i -> i % 2 == 0)
                .forEach(n -> {
                    String expectedValue = keyAndValueList.get(n + 1);
                    String expectedKey = keyAndValueList.get(n);
                    String actualValue = testMethodConfiguration.get(expectedKey).detach().asString().get();
                    assertEquals(expectedValue, actualValue);
                });

        assertPartialMap(testServer.config().detach().asMap().get(), testMethodConfiguration.detach().asMap().get());
    }

    static void assertPartialMap(Map<String, String> sourceMap, Map<String, String> containsMap) {
        containsMap.entrySet()
                .stream()
                .filter(e -> !e.getKey().startsWith("webserver"))
                .forEach(e -> {
                    assertTrue(sourceMap.containsKey(e.getKey()), "Source map does not contain key: " + e.getKey());
                    assertEquals(sourceMap.get(e.getKey()), e.getValue());
                });
    }
}
