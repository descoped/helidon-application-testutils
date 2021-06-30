package io.descoped.helidon.application.test.scenario;

import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.server.ConfigurationOverride;
import io.descoped.helidon.application.test.server.ConfigurationProfile;
import io.descoped.helidon.application.test.server.Deployment;
import io.descoped.helidon.application.test.server.TestServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(TestServerExtension.class)
@Deployment(target = SuiteDeployment.class)
public class CaseMixedConfigurationTest {

    private static final Logger LOG = LoggerFactory.getLogger(CaseMixedConfigurationTest.class);

    @Inject
    TestClient client;

    @Test
    public void thatExtensionIsInvoked() {
        System.out.println("Hello 2");
    }

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
    @ConfigurationOverride({"foo", "bar"})
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
