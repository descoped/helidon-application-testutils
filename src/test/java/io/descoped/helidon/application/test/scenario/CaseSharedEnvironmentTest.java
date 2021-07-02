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

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(TestServerExtension.class)
public class CaseSharedEnvironmentTest {

    @Deployment
    public static HelidonDeployment createDeployment() {
        return HelidonDeployment.newBuilder()
                .configSource(ConfigSources.classpath("application-test.yaml").optional(true).build())
                .routing(builder ->
                        builder.get("/greet", (req, res) ->
                                res.send("Hello " + TestServer.instance()
                                        .config().get("message").asString().orElseThrow(IllegalArgumentException::new)))
                )
                .build();
    }

    @Test
    @ConfigurationOverride({"message", "World!"})
    public void testServer() {
        ResponseHelper<String> response = TestClient.instance().get("/greet").expect200Ok();
        assertEquals("Hello " + TestServer.instance().config().get("message").asString().get(), response.body());
    }
}
