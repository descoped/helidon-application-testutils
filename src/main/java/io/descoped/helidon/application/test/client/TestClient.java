package io.descoped.helidon.application.test.client;

import io.descoped.helidon.application.test.server.TestServerFactory;
import io.descoped.helidon.application.test.support.TestUriResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

public final class TestClient {

    private static final Logger LOG = LoggerFactory.getLogger(TestClient.class);

    private final TestUriResolver server;
    private final HttpClient client;

    private TestClient(TestUriResolver server) {
        this.server = server;
        this.client = HttpClient.newBuilder().build();
    }

    public static TestClient create(TestUriResolver server) {
        return new TestClient(server);
    }

    public static TestClient instance() {
        return TestServerFactory.instance().currentClient();
    }

    public TestUriResolver getTestUriResolver() {
        return server;
    }

    public ResponseHelper<String> options(String uri, String... headersKeyAndValue) {
        return options(uri, HttpResponse.BodyHandlers.ofString(), headersKeyAndValue);
    }

    public <R> ResponseHelper<R> options(String uri, HttpResponse.BodyHandler<R> bodyHandler, String... headersKeyAndValue) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                    .headers(headersKeyAndValue)
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new TestClientException(e);
        }
    }

    public ResponseHelper<String> head(String uri) {
        return head(uri, HttpResponse.BodyHandlers.ofString());
    }

    public <R> ResponseHelper<R> head(String uri, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new TestClientException(e);
        }
    }

    public ResponseHelper<String> put(String uri) {
        return put(uri, HttpRequest.BodyPublishers.noBody(), HttpResponse.BodyHandlers.ofString());
    }

    public ResponseHelper<String> put(String uri, String body) {
        return put(uri, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString());
    }

    public <R> ResponseHelper<R> put(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                    .PUT(bodyPublisher)
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new TestClientException(e);
        }
    }

    public ResponseHelper<String> post(String uri) {
        return post(uri, HttpRequest.BodyPublishers.noBody(), HttpResponse.BodyHandlers.ofString());
    }

    public ResponseHelper<String> post(String uri, String body) {
        return post(uri, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString());
    }

    public <R> ResponseHelper<R> post(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                    .POST(bodyPublisher)
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new TestClientException(e);
        }
    }

    public <R> ResponseHelper<R> postJson(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                    .POST(bodyPublisher)
                    .header("Content-Type", "application/json")
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new TestClientException(e);
        }
    }

    public <R> ResponseHelper<R> postForm(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                    .POST(bodyPublisher)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new TestClientException(e);
        }
    }

    public ResponseHelper<String> get(HttpRequests.Get get) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(server.testURL(get.uri())))
                    .GET();
            if (get.headers().length > 0) {
                requestBuilder.headers(get.headers());
            }
            return new ResponseHelper<>(client.send(requestBuilder.build(), get.bodyHandler()));
        } catch (Exception e) {
            LOG.error("Error: {}\n{}", server.testURL(get.uri()), captureStackTrace(e));
            throw new TestClientException(e);
        }
    }

    public ResponseHelper<String> get(String uri, String... headers) {
        return get(uri, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8), headers);
    }

    @SuppressWarnings("unchecked")
    public <R> ResponseHelper<R> get(String uri, HttpResponse.BodyHandler<R> bodyHandler, String... headers) {
        HttpRequests.Get.Builder builder = HttpRequests.newGetBuilder()
                .uri(uri)
                .bodyHandler(bodyHandler);
        convertHeaderKeyValuePairToMap(headers).forEach(builder::header);
        return (ResponseHelper<R>) get(builder.build());
    }

    public ResponseHelper<String> delete(String uri) {
        return delete(uri, HttpResponse.BodyHandlers.ofString());
    }

    public <R> ResponseHelper<R> delete(String uri, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                    .DELETE()
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new TestClientException(e);
        }
    }

    static Map<String, String> convertHeaderKeyValuePairToMap(String... keyValue) {
        Map<String, String> headerMap = new LinkedHashMap<>();
        IntStream.range(0, keyValue.length)
                .filter(i -> i % 2 == 0)
                .forEach(n -> headerMap.put(keyValue[n], keyValue[n + 1]));
        return headerMap;
    }

    static String captureStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
