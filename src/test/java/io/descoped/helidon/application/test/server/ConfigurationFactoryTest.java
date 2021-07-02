package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.base.ConfigHelper;
import io.helidon.config.Config;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationFactoryTest {

    @Test
    void testConfigurationKey() {
        ClassOrMethodIdentifier classKey = ClassOrMethodIdentifier.from("ClassA");
        assertEquals("ClassA", classKey.className);

        ClassOrMethodIdentifier methodKey = ClassOrMethodIdentifier.from("ClassA", "method");
        assertEquals("ClassA", methodKey.className);
        assertEquals("method", methodKey.methodName);
    }

    @Test
    void thatKeysAreNotDuplicated() {
        Configurations factory = new Configurations();

        factory.register(new ConfigurationTarget.Builder().testIdentifier(ClassOrMethodIdentifier.from("ClassA")).configuration(new Configurations.ConfigurationProfile("profile1")).build());
        assertEquals(1, factory.keySet().size());

        assertThrows(IllegalStateException.class, () -> {
            factory.register(new ConfigurationTarget.Builder().testIdentifier(ClassOrMethodIdentifier.from("ClassA")).configuration(new Configurations.ConfigurationProfile("profile1")).build());
            assertEquals(1, factory.keySet().size());
        });

        factory.register(new ConfigurationTarget.Builder().testIdentifier(ClassOrMethodIdentifier.from("ClassA", "method1")).configuration(new Configurations.ConfigurationProfile("profile2")).build());
        assertEquals(2, factory.keySet().size());

        assertThrows(IllegalStateException.class, () -> {
            factory.register(new ConfigurationTarget.Builder().testIdentifier(ClassOrMethodIdentifier.from("ClassA", "method1")).configuration(new Configurations.ConfigurationProfile("profile2")).build());
            assertEquals(2, factory.keySet().size());
        });

        factory.keySet().clear();
        assertEquals(0, factory.keySet().size());
    }

    @Test
    void testConfigurationProfileIsLoaded() {
        Config.Builder builder = ConfigHelper.createEmptyConfigBuilder();

        Configurations.ConfigurationProfile configurationProfile = new Configurations.ConfigurationProfile("dummy");
        configurationProfile.copyTo(builder);

        Config configuration = builder.build();

        assertEquals("v1", configuration.get("k1").asString().get());

        assertEquals(configurationProfile, new Configurations.ConfigurationProfile("dummy"));
        assertEquals(configurationProfile.hashCode(), Objects.hash("dummy"));
        assertNotNull(configurationProfile.toString());
    }

    @Test
    void testConfigurationOverrideIsLoaded() {
        Config.Builder builder = ConfigHelper.createEmptyConfigBuilder();

        Map<String, String> map = Map.of("k1", "v1");
        Configurations.ConfigurationOverride configurationOverride = new Configurations.ConfigurationOverride(map);
        configurationOverride.copyTo(builder);

        Config configuration = builder.build();

        assertEquals("v1", configuration.get("k1").asString().get());

        assertEquals(configurationOverride, new Configurations.ConfigurationOverride(map));
        assertEquals(configurationOverride.hashCode(), map.hashCode());
        assertNotNull(configurationOverride.toString());
    }

    @Test
    void testConfigurationProfileAndOverrideAreLoaded() {
        Config.Builder builder = ConfigHelper.createEmptyConfigBuilder();

        Configurations.ConfigurationProfile configurationProfile = new Configurations.ConfigurationProfile("dummy");
        configurationProfile.copyTo(builder);

        Map<String, String> map = Map.of("k2", "v2");
        Configurations.ConfigurationOverride configurationOverride = new Configurations.ConfigurationOverride(map);
        configurationOverride.copyTo(builder);

        Config configuration = builder.build();

        assertEquals("v1", configuration.get("k1").asString().get());
        assertEquals("v2", configuration.get("k2").asString().get());

        assertEquals(configurationOverride, new Configurations.ConfigurationOverride(map));
        assertEquals(configurationOverride.hashCode(), map.hashCode());
        assertNotNull(configurationOverride.toString());
    }
}
