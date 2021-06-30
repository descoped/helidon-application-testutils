package io.descoped.helidon.application.test.client;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HttpRequests {

    public static Get.Builder newGetBuilder() {
        return new Get.Builder();
    }

    public static class Get {

        private final String uri;
        private final Map<String, String> headerMap;
        private final HttpResponse.BodyHandler<?> bodyHandler;

        public Get(String uri, Map<String, String> headerMap, HttpResponse.BodyHandler<?> bodyHandler) {
            this.uri = uri;
            this.headerMap = headerMap;
            this.bodyHandler = bodyHandler;
        }

        public String uri() {
            return uri;
        }

        public String[] headers() {
            List<String> headerNameValueList = new ArrayList<>();
            headerMap.forEach((key, value) -> {
                headerNameValueList.add(key);
                headerNameValueList.add(value);
            });
            return headerNameValueList.toArray(new String[0]);
        }

        @SuppressWarnings("unchecked")
        public <R> HttpResponse.BodyHandler<R> bodyHandler() {
            return (HttpResponse.BodyHandler<R>) bodyHandler;
        }

        public static class Builder {

            private String uri;
            private Map<String, String> headerMap = new LinkedHashMap<>();
            private HttpResponse.BodyHandler<?> bodyHandler;

            public Builder uri(String uri) {
                this.uri = uri;
                return this;
            }

            public Builder header(String name, String value) {
                headerMap.put(name, value);
                return this;
            }

            public <R> Builder bodyHandler(HttpResponse.BodyHandler<R> bodyHandler) {
                this.bodyHandler = bodyHandler;
                return this;
            }

            public Get build() {
                Objects.requireNonNull(uri, "Missing uri!");
                Objects.requireNonNull(bodyHandler, "Missing BodyHandler!");
                return new Get(uri, headerMap, bodyHandler);
            }
        }
    }
}
