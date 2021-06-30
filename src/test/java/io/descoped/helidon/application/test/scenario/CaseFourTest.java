package io.descoped.helidon.application.test.scenario;

import io.descoped.helidon.application.base.HelidonDeployment;
import io.descoped.helidon.application.test.client.ResponseHelper;
import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.server.ConfigurationOverride;
import io.descoped.helidon.application.test.server.Deployment;
import io.descoped.helidon.application.test.server.TestServer;
import io.descoped.helidon.application.test.server.TestServerExtension;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(TestServerExtension.class)
public class CaseFourTest {
    static final Logger LOG = LoggerFactory.getLogger(CaseFourTest.class);

    @Inject
    TestClient client;

    @Inject
    TestServer server;

    @Deployment
    public static HelidonDeployment createDeployment() {
        return HelidonDeployment.newBuilder()
                .configSource(ConfigSources.classpath("application-test.yaml").optional(true).build())
                .routing(builder ->
                        builder.get("/greet", (req, res) ->
                                res.send("Hello " + req.queryParams().first("message").orElseThrow())))
                .build();
    }

    @Test
    @ConfigurationOverride({"message", "World!"})
    public void testServer() {
        String message = server.getConfiguration().get("message").asString().get();
        ResponseHelper<String> response = client.get("/greet?message=" + message).expect200Ok();
        assertEquals("Hello " + message, response.body());
    }
}
