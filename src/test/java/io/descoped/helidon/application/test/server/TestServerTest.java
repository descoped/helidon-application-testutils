package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.base.HelidonDeployment;
import io.descoped.helidon.application.test.client.ResponseHelper;
import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.scenario.MethodDeployment;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(TestServerExtension.class)
public class TestServerTest {

    static final Logger LOG = LoggerFactory.getLogger(TestServerTest.class);

    @Inject
    TestClient client;

    @Inject
    TestServer server;

    @Deployment
    static HelidonDeployment createDeployment() {
        return HelidonDeployment.newBuilder()
                .configSource(ConfigSources.classpath("application-test.yaml").build())
                .routing(builder -> builder.get("/greet", (req, res) -> res.send("Hello World!")))
                .build();
    }

    @Test
    void testServer() {
        ResponseHelper<String> response = client.get("/greet").expect200Ok();
        LOG.trace("{}", response.body());
        assertEquals("Hello World!", response.body());
    }

    @Test
    @Deployment(target = MethodDeployment.class)
    void testServer2() throws InterruptedException {
        ResponseHelper<String> response = client.get("/greet2").expect200Ok();
        LOG.trace("{}", response.body());
        assertEquals("Hello there!", response.body());
    }
}
