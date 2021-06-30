package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.base.ConfigHelper;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.Source;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class ConfigurationTarget implements CompareTargetState<ConfigurationTarget>, DeploymentLifecycle {

    private Map<ClassOrMethodIdentifier, Map<String, String>> computedConfigCache;

    final ClassOrMethodIdentifier testIdentifier;
    final List<Configurations.Configuration> configurationList;
    private final Context context;
    private List<Source.Builder<?>> configSources;
    private Config.Builder configBuilder;

    private ConfigurationTarget(ClassOrMethodIdentifier testIdentifier, List<Configurations.Configuration> configurationList) {
        this.testIdentifier = testIdentifier;
        this.configurationList = configurationList;
        context = testIdentifier.isMethod() ? Context.METHOD : Context.CLASS;
    }

    ConfigurationTarget initialize(Configurations configurations,
                                   Supplier<Map<ClassOrMethodIdentifier, Map<String, String>>> computedConfigCacheSupplier) {
        computedConfigCache = computedConfigCacheSupplier.get();

        // create config builder
        configSources = createConfigSources();
        configBuilder = createConfigBuilder();
        // build current config builder
        for (Configurations.Configuration configuration : configurationList) {
            if (configuration instanceof Configurations.ConfigurationProfile) {
                Configurations.ConfigurationProfile configurationProfile = (Configurations.ConfigurationProfile) configuration;
                configSources.add(ConfigSources.classpath(configurationProfile.profileName));
            }
            configuration.copyTo(configBuilder);
        }
        // build ancestor config
        if (testIdentifier.isMethod()) {
            ConfigurationTarget classLevelConfigurationTarget = configurations.tryGet(ClassOrMethodIdentifier.from(testIdentifier.className));
            if (classLevelConfigurationTarget != null) {
                for (Configurations.Configuration configuration : classLevelConfigurationTarget.configurationList) {
                    configuration.copyTo(configBuilder);
                }
            }
        }
        return this;
    }

    private void checkIfInitialized() {
        Objects.requireNonNull(computedConfigCache, "Computed Configuration Cache NOT initialized!");
        Objects.requireNonNull(configBuilder, "Configuration NOT initialized!");
    }

    private List<Source.Builder<?>> createConfigSources() {
        List<Source.Builder<?>> defaultSources = new ArrayList<>();
        defaultSources.add(ConfigSources.classpath("application-defaults.yaml"));
        defaultSources.add(ConfigSources.classpath("application-test.yaml"));
        return defaultSources;
    }

    private Config.Builder createConfigBuilder() {
        return ConfigHelper.createEmptyConfigBuilder()
                .sources(
                        ConfigSources.classpath("application-defaults.yaml"),
                        ConfigSources.classpath("application-test.yaml").optional()
                )
                ;
    }

    @Override
    public Context context() {
        checkIfInitialized();
        return context;
    }

    public Config.Builder toConfigBuilder() {
        checkIfInitialized();
        return configBuilder;
//        return Config.builder(ConfigSources.create(configBuilder.build())).disableSystemPropertiesSource().disableEnvironmentVariablesSource();
    }

    public Config toConfig() {
        return toConfigBuilder().build();
    }

    public Map<String, String> toConfigMap() {
        return computedConfigCache.computeIfAbsent(this.testIdentifier, ignore -> this.toConfig().asMap().get())
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    @Override
    public boolean identicalTo(ConfigurationTarget other) {
//        List<String> thisProfileList = new ArrayList<>();
//        List<String> thatProfileList = new ArrayList<>();
//        Map<String, String> thisOverrideMap = new LinkedHashMap<>();
//        Map<String, String> thatOverrideMap = new LinkedHashMap<>();
//        for (Configurations.Configuration configuration : configurationList) {
//            if (configuration instanceof Configurations.ConfigurationProfile) {
//                Configurations.ConfigurationProfile configurationProfile = (Configurations.ConfigurationProfile) configuration;
//                thisProfileList.add(configurationProfile.profileName);
//            } else if (configuration instanceof Configurations.ConfigurationOverride) {
//                Configurations.ConfigurationOverride configurationOverride = (Configurations.ConfigurationOverride) configuration;
//                thisOverrideMap.putAll(configurationOverride.overrideMap);
//            } else {
//                throw new IllegalStateException();
//            }
//        }
//        for (Configurations.Configuration configuration : other.configurationList) {
//            if (configuration instanceof Configurations.ConfigurationProfile) {
//                Configurations.ConfigurationProfile configurationProfile = (Configurations.ConfigurationProfile) configuration;
//                thatProfileList.add(configurationProfile.profileName);
//            } else if (configuration instanceof Configurations.ConfigurationOverride) {
//                Configurations.ConfigurationOverride configurationOverride = (Configurations.ConfigurationOverride) configuration;
//                thatOverrideMap.putAll(configurationOverride.overrideMap);
//            } else {
//                throw new IllegalStateException();
//            }
//        }
//        return thisProfileList.equals(thatProfileList) && thisOverrideMap.equals(thatOverrideMap);
        return this.toConfigMap().equals(other.toConfigMap());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationTarget that = (ConfigurationTarget) o;
//        return testIdentifier.equals(that.testIdentifier) && configurationList.equals(that.configurationList);
        return testIdentifier.equals(that.testIdentifier) && identicalTo(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testIdentifier, configurationList);
    }

    @Override
    public String toString() {
        return "ConfigurationTarget{" +
                "testTarget=" + testIdentifier +
                ", configurationList=" + configurationList +
                '}';
    }

    static class Builder {

        private ClassOrMethodIdentifier testIdentifier;
        private final List<Configurations.Configuration> configurationList = new ArrayList<>();

        Builder testIdentifier(ClassOrMethodIdentifier testIdentifier) {
            this.testIdentifier = testIdentifier;
            return this;
        }

        Builder configuration(Configurations.Configuration configuration) {
            configurationList.add(configuration);
            return this;
        }

        ConfigurationTarget build() {
            return new ConfigurationTarget(testIdentifier, configurationList);
        }
    }
}
