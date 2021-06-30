# Helidon Application TestUtils

TestUtils is a comprehensive and simple use test library for testing out pieces of your code base at a time.

## Example of use

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
