# Helidon Application TestUtils

TestUtils is a comprehensive and simple use test library for testing out pieces of your code base at a time.

## Example of use

### Use case #1

```java

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
}    
```

### Use case #2

```java
public class MethodDeployment {

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
}
```

```java
import io.descoped.helidon.application.test.client.TestClient;
import io.descoped.helidon.application.test.server.TestServer;

@ExtendWith(TestServerExtension.class)
public class TestServerTest {

    static final Logger LOG = LoggerFactory.getLogger(TestServerTest.class);

    @Test
    @Deployment(target = MethodDeployment.class)
    @ConfigurationOverride({"message", "World!"})
    void testServer() {
        ResponseHelper<String> response = TestClient.instance().get("/greet").expect200Ok();
        assertEquals("Hello " + TestServer.instance().config().get("message").asString().get(), response.body());
    }
}    
```
