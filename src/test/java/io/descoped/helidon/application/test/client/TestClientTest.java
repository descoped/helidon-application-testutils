package io.descoped.helidon.application.test.client;

import io.descoped.helidon.application.test.scenario.SuiteDeployment;
import io.descoped.helidon.application.test.server.Deployment;
import io.descoped.helidon.application.test.server.TestServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(TestServerExtension.class)
@Deployment(target = SuiteDeployment.class)
public class TestClientTest {

    @Inject
    TestClient client;

    @Test
    void testGet() {
        ResponseHelper<String> response = TestClient.instance().get("/greet").expect200Ok();
        assertEquals("Hello World!", response.body());
    }

    @Test
    void testGetBuilder() {
        ResponseHelper<String> response = client.get(HttpRequests.newGetBuilder()
                .uri("/greet")
                .bodyHandler(HttpResponse.BodyHandlers.ofString())
                .build())
                .expect200Ok();
        assertEquals("Hello World!", response.body());
    }

}
