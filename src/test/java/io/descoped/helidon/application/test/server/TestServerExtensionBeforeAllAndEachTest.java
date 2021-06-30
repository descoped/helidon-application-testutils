package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.scenario.SuiteDeployment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(TestServerExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Deployment(target = SuiteDeployment.class)
public class TestServerExtensionBeforeAllAndEachTest {

    @Inject
    TestClient testClient;

    @Inject
    TestServer testServer;

    @BeforeAll
    static void beforeAll() {
        assertNotNull(TestServer.currentInstance());
        assertNotNull(TestClient.currentInstance());
    }

    @BeforeEach
    void beforeEach() {
        assertEquals(testServer, TestServer.currentInstance());
        assertEquals(testClient, TestClient.currentInstance());
    }

    @Test
    void testRootScoped() {
        assertEquals(testServer, TestServer.currentInstance());
        assertEquals(testClient, TestClient.currentInstance());
    }
}
