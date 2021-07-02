package io.descoped.helidon.application.test.scenario;

import io.descoped.helidon.application.base.HelidonDeployment;
import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.server.ConfigurationOverride;
import io.descoped.helidon.application.test.server.ConfigurationProfile;
import io.descoped.helidon.application.test.server.Deployment;
import io.descoped.helidon.application.test.server.TestServerExtension;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(TestServerExtension.class)
@ConfigurationOverride({"foo", "bar"})
public class CaseMethodDeploymentTest {

    private static final Logger LOG = LoggerFactory.getLogger(CaseMethodDeploymentTest.class);

    @Inject
    TestClient client;

    @Deployment
    public static HelidonDeployment createDeployment() {
        return HelidonDeployment.newBuilder()
                .configSource(ConfigSources.classpath("application-test.yaml").optional(true).build())
                .routing(builder -> builder.get("/greet", (req, res) -> res.send("Hello World!")))
                .build();
    }

    @Test
    public void thatExtensionIsInvoked() {
        System.out.println("Hello 2");
    }

    @Deployment(target = CaseMethodDeploymentTest.class)
    @ConfigurationOverride({"foo", "bar"})
    @Test
    public void thatMethodOverrideConfigurationIsCreated() {

    }

    @ConfigurationProfile("dummy")
    @ConfigurationOverride({"foo", "bar"})
    @Test
    public void thatMethodOverrideProfileConfigurationIsCreated() {

    }

    @ConfigurationProfile("dummy")
    @ConfigurationOverride({"foo", "baz"})
    @Test
    public void thatMethodOverrideProfileConfiguration2IsCreated() {

    }

    @Test
    public void handleException() {
        assertThrows(RuntimeException.class, () -> {
            throw new RuntimeException("Blow!");
        });
    }
}
