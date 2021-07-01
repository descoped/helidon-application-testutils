package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.base.DefaultHelidonApplication;
import io.descoped.helidon.application.base.HelidonApplication;
import io.descoped.helidon.application.base.HelidonDeployment;
import io.descoped.helidon.application.test.support.TestUriResolver;
import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.config.OverrideSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class TestServer implements TestUriResolver {

    static final Logger LOG = LoggerFactory.getLogger(TestServer.class);

    private final Config configuration;
    private final HelidonApplication application;

    public TestServer(HelidonDeployment deployment) {
        this.application = new DefaultHelidonApplication(deployment);
        this.configuration = this.application.configuration();
    }

    public static TestServer instance() {
        return TestServerFactory.instance().currentServer();
    }

    public Single<? extends HelidonApplication> start() {
        return application.start();
    }

    public Single<? extends HelidonApplication> stop() {
        return application.stop();
    }

    public String protocol() {
        return application.getWebServerInfo().protocol();
    }

    public String host() {
        return application.getWebServerInfo().host();
    }

    public int port() {
        return application.getWebServerInfo().port();
    }

    @Override
    public String testURL(String uri) {
        try {
            URL url = new URL(protocol(), host(), port(), uri);
            return url.toExternalForm();
        } catch (MalformedURLException e) {
            throw new TestServerException(e);
        }
    }

    public Config config() {
        return configuration;
    }

    public HelidonApplication getApplication() {
        return application;
    }

    @SuppressWarnings("SameParameterValue")
    static int findFreePort(Random random, int from, int to) {
        int port = pick(random, from, to);
        for (int i = 0; i < 2 * ((to + 1) - from); i++) {
            if (isLocalPortFree(port)) {
                return port;
            }
            port = pick(random, from, to);
        }
        throw new IllegalStateException("Unable to find any available ports in range: [" + from + ", " + (to + 1) + ")");
    }

    private static int pick(Random random, int from, int to) {
        return from + random.nextInt((to + 1) - from);
    }

    private static boolean isLocalPortFree(int port) {
        try {
            try (ServerSocket ignore = new ServerSocket(port)) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    public static class Builder {
        private HelidonDeployment.Builder deploymentBuilder;
        private Integer port = -1;

        public Builder deployment(HelidonDeployment.Builder deploymentBuilder) {
            this.deploymentBuilder = deploymentBuilder;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public TestServer build() {
            Objects.requireNonNull(deploymentBuilder);

            if (Set.of(-1, 0, 9990).stream().anyMatch(p -> port.equals(p))) {
                port = findFreePort(new SecureRandom(), 9000, 9499);
            }

            deploymentBuilder.overrideSource(OverrideSources.create(Map.of(deploymentBuilder.webserverConfigProperty() + ".port", port.toString())));

            HelidonDeployment helidonDeployment = deploymentBuilder.build();

            return new TestServer(helidonDeployment);
        }
    }
}
