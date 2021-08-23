package io.descoped.helidon.application.test.server;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.MapConfigSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class Configurations {
    
    private final Map<ClassOrMethodIdentifier, ConfigurationTarget> configurationMap = new ConcurrentHashMap<>();
    private final Map<ClassOrMethodIdentifier, Map<String, String>> computedConfigCache = new LinkedHashMap<>(); // cache built helidon config maps (see ConfigurationTarget.identicalTo)

    private Map<ClassOrMethodIdentifier, Map<String, String>> provideSharedConfigCache() {
        return computedConfigCache;
    }

    void register(ConfigurationTarget configurationTarget) {
        if (configurationMap.putIfAbsent(configurationTarget.testIdentifier, configurationTarget.initialize(this, this::provideSharedConfigCache)) != null) {
            throw new IllegalStateException(configurationTarget.testIdentifier + " already exists!");
        }
    }

    /**
     * Try ConfigurationTarget will try to retrieve applied testIdentifier and if not present it will fallback
     * and try to obtain by Class level identifier.
     *
     * @param testIdentifier
     * @return
     */
    ConfigurationTarget tryGet(ClassOrMethodIdentifier testIdentifier) {
        // method match
        if (configurationMap.containsKey(testIdentifier)) {
            return configurationMap.get(testIdentifier);
        }
        // try class match
        return configurationMap.get(ClassOrMethodIdentifier.from(testIdentifier.className));
    }

    public Set<ClassOrMethodIdentifier> keySet() {
        return configurationMap.keySet();
    }

    /**
     * Search for first matching key in computed config cache. There is no guarantee that it finds the exact match
     * as two different configurations may have different config values.
     *
     * @param key
     * @return
     */
    public String tryFindConfigPropertyAsString(String key) {
        for (Map<String, String> configMap : computedConfigCache.values()) {
            if (configMap.containsKey(key)) {
                return configMap.get(key);
            }
        }
        return null;
    }

    abstract static class Configuration {
        abstract void copyTo(Config.Builder builder);
    }

    static class ConfigurationProfile extends Configuration {

        final String profileName;

        ConfigurationProfile(String profileName) {
            this.profileName = profileName;
        }

        @Override
        void copyTo(Config.Builder builder) {
            builder.addSource(ConfigSources.classpath(String.format("application-%s.yaml", profileName)));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigurationProfile that = (ConfigurationProfile) o;
            return profileName.equals(that.profileName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(profileName);
        }

        @Override
        public String toString() {
            return "ConfigurationProfile{" +
                    "profileName='" + profileName + '\'' +
                    '}';
        }
    }

    static class ConfigurationOverride extends Configuration {
        final Map<String, String> overrideMap;

        ConfigurationOverride(Map<String, String> overrideMap) {
            this.overrideMap = overrideMap;
        }

        @Override
        void copyTo(Config.Builder builder) {
            builder.addSource(MapConfigSource.create(overrideMap));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigurationOverride that = (ConfigurationOverride) o;
            return overrideMap.equals(that.overrideMap);
        }

        @Override
        public int hashCode() {
            return overrideMap.hashCode();
        }

        @Override
        public String toString() {
            return "ConfigurationOverride{" +
                    "overrideMap=" + overrideMap +
                    '}';
        }
    }
}
