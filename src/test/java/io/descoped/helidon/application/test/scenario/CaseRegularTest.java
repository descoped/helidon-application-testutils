package io.descoped.helidon.application.test.scenario;

import io.descoped.helidon.application.base.HelidonDeployment;
import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.server.Deployment;
import io.descoped.helidon.application.test.server.TestServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(TestServerExtension.class)
public class CaseRegularTest {

    private static final Logger LOG = LoggerFactory.getLogger(CaseRegularTest.class);

    @Inject
    TestClient client;

    @Deployment
    static HelidonDeployment createDeployment() {
        return HelidonDeployment.newBuilder()
                .routing(builder -> builder.get("/greet", (req, res) -> res.send("Hello World!")))
                .build();
    }

    @Test
    public void thatExtensionIsInvoked() {
        assertNotNull(client);
        LOG.trace("TestURL: {}", client.getTestUriResolver().testURL(""));
    }
}
