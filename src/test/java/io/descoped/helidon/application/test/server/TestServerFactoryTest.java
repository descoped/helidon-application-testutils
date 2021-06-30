package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.scenario.MethodDeployment;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TestServerFactoryTest {

    static final Logger LOG = LoggerFactory.getLogger(TestServerFactoryTest.class);

    @Test
    void testBuilder() {
        ClassOrMethodIdentifier testMethodIdentifier = ClassOrMethodIdentifier.from("dummy", "test");

        Deployments deployments = new Deployments();
        deployments.register(new DeploymentTarget.Builder()
                .testIdentifier(testMethodIdentifier)
                .deploymentIdentifier(ClassOrMethodIdentifier.from(MethodDeployment.class, "createDeployment"))
                .build());

        Configurations configurations = new Configurations();
        configurations.register(new ConfigurationTarget.Builder()
                .testIdentifier(testMethodIdentifier)
                .build());

        TestMethods testMethods = new TestMethods(deployments, configurations);
        testMethods.register(new TestMethod(testMethodIdentifier));

        ExecutionPlan executionPlan = new ExecutionPlan(testMethods);

        TestServerFactory factory = TestServerFactory.createInitializer(TestServerFactory.Instance.TEST_SERVER_FACTORY)
                .initialize(deployments, configurations, executionPlan);

        TestServer testServer = factory.findTestServerSuppliersForMethodContextByMethodIdentifier(testMethodIdentifier).getValue().get();
        testServer.start()
                .toCompletableFuture()
                .orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    LOG.error("While starting application", throwable);
                    System.exit(1);
                    return null;
                });

        TestClient client = TestClient.create(testServer);

        LOG.trace("{}", client.get("/greet2").expect200Ok().body());

        testServer.stop()
                .toCompletableFuture()
                .orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    LOG.error("While shutting down application", throwable);
                    System.exit(1);
                    return null;
                });
    }
}
