package io.descoped.helidon.application.test.scenario;

import io.descoped.helidon.application.base.HelidonDeployment;
import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.server.Deployment;
import io.descoped.helidon.application.test.server.TestServerExtension;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@ExtendWith(TestServerExtension.class)
public class CaseNormalTest {

    private static final Logger LOG = LoggerFactory.getLogger(CaseNormalTest.class);

    @Inject
    TestClient client;

    @Deployment
    static HelidonDeployment createDeployment() {
        return HelidonDeployment.newBuilder()
                .configSource(ConfigSources.classpath("application-test.yaml").optional(true).build())
                .routing(builder -> builder.get("/greet", (req, res) -> res.send("Hello World!")))
                .build();
    }

    @Test
    public void thatExtensionIsInvoked() {
        LOG.trace("TestURL: {}", client.getTestUriResolver().testURL(""));
    }
}
