package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.base.ConfigHelper;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class ConfigurationTarget implements CompareTargetState<ConfigurationTarget>, DeploymentLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationTarget.class);

    private Map<ClassOrMethodIdentifier, Map<String, String>> computedConfigCache;

    final ClassOrMethodIdentifier testIdentifier;
    final List<Configurations.Configuration> configurationList;
    private final Context context;
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
        configBuilder = createConfigBuilder();

        // copy configuration to configBuilder
        for (Configurations.Configuration configuration : this.configurationList) {
            configuration.copyTo(configBuilder);
            LOG.warn("initialize: --> target: " + testIdentifier + "\n" + configuration);
        }

        // copy ancestor class configuration to configBuilder
        if (testIdentifier.isMethod()) {
            ConfigurationTarget classLevelConfigurationTarget = configurations.tryGet(ClassOrMethodIdentifier.from(testIdentifier.className));
            if (classLevelConfigurationTarget != null) {
                for (Configurations.Configuration configuration : classLevelConfigurationTarget.configurationList) {
                    if (this.configurationList.contains(configuration)) {
                        continue;
                    }
                    configuration.copyTo(configBuilder);
                }
            }
        }

        return this;
    }

    private void checkIfInitialized() {
        Objects.requireNonNull(configBuilder, "Configuration NOT initialized!");
        Objects.requireNonNull(computedConfigCache, "Computed Configuration Cache NOT initialized!");
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
    }

    public Config toConfig() {
        return toConfigBuilder().build();
    }

    public Map<String, String> toConfigMap() {
        final LinkedHashMap<String, String> collect = computedConfigCache.computeIfAbsent(this.testIdentifier, ignore -> this.toConfig().asMap().get())
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        LOG.warn("toConfigMap(): " + testIdentifier.toString() + "\n" + collect.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("\n")));
        return collect;
    }

    @Override
    public boolean identicalTo(ConfigurationTarget other) {
        return this.toConfigMap().equals(other.toConfigMap());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationTarget that = (ConfigurationTarget) o;
//        return testIdentifier.equals(that.testIdentifier) && identicalTo(that);
        return testIdentifier.equals(that.testIdentifier) && configurationList.equals(that.configurationList);
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
