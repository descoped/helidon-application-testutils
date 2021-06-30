package io.descoped.helidon.application.test.client;

public class TestClientException extends RuntimeException {

    public TestClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestClientException(Throwable cause) {
        super(cause);
    }

    public TestClientException(String message) {
        super(message);
    }
}
