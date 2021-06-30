package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.base.ConfigHelper;
import io.helidon.config.ClasspathConfigSource;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigTest {

    static final Logger LOG = LoggerFactory.getLogger(ConfigTest.class);

    @Test
    void testBuildConfig() {
        Config.Builder configBuilder = ConfigHelper.createEmptyConfigBuilder()
                .sources(ConfigSources.classpath("application-defaults.yaml"))
                .addSource(ConfigSources.classpath("application-test.yaml").optional(true))
                ;

        LOG.trace("passthrough create: {}", Config.create().asMap().get());
        LOG.trace("configBuilder: {}", configBuilder.build().asMap().get());
        LOG.trace("configBuilder get 'webserver.port': '{}'", configBuilder.build().get("webserver.port").asString());

        Config.Builder configBuilder2 = Config.builder(ConfigSources.create(configBuilder.build()));

        LOG.trace("configBuilder2: {}", configBuilder2.build().asMap().get());

        assertEquals(configBuilder.build().asMap().get(), configBuilder2.build().asMap().get());

        Map<String, String> map = Map.of("k1", "k2");

        Config override = Config.create(ConfigSources.create(map).build());

        ConfigNode.ObjectNode nodeList = ConfigNode.ObjectNode.builder().addNode("k1", ConfigNode.ValueNode.create("v2")).build();

//        configBuilder.addSource(ConfigSources.create(override));
        configBuilder.addSource(ConfigSources.create(nodeList));

        LOG.trace("configBuilder update1: {}", configBuilder.build().asMap().get());
        LOG.trace("configBuilder get 'webserver.port': '{}'", configBuilder.build().get("webserver.port").asString());
        LOG.trace("configBuilder get 'k1': '{}'", configBuilder.build().get("k1").asString());
    }

    // try to recreate: io.helidon.config.ConfigException: Attempting to load a single config source multiple times. This is a bug.
    @Disabled
    @Test
    void tryRecreateConfigSourceEquals() {
        ClasspathConfigSource.Builder c1 = ConfigSources.classpath("application-defaults.yaml");
        ClasspathConfigSource.Builder c2 = ConfigSources.classpath("application-defaults.yaml");
        ClasspathConfigSource.Builder c3 = ConfigSources.classpath("application-defaults.yaml");

        Config.Builder configBuilder = ConfigHelper.createEmptyConfigBuilder();
        Config config = configBuilder.sources(c1, c2).build();

        Config.Builder configBuilder2 = ConfigHelper.createEmptyConfigBuilder();
        configBuilder2.sources(ConfigSources.create(config));

        Config.Builder config2 = configBuilder2.addSource(c3);
        LOG.trace("---> c1: {}", config2.build().asMap().get());
    }
}
