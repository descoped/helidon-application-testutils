package io.descoped.helidon.application.test.server;

import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

class ExecutionKey {

    final ClassOrMethodIdentifier deploymentIdentifier;
    final Context context;
    final ConfigurationTarget configurationTarget;
    final Map<String, String> configMap;

    private ExecutionKey(ClassOrMethodIdentifier deploymentIdentifier, Context context, ConfigurationTarget configurationTarget) {
        Objects.requireNonNull(deploymentIdentifier.methodName, "DeploymentIdentifier.methodName IS NULL");
        this.deploymentIdentifier = deploymentIdentifier;
        this.context = context;
        this.configurationTarget = configurationTarget;
        this.configMap = configurationTarget.toConfigMap();
    }

    ExecutionKey copyAndUpdateContext(Context newContext) {
        return new Builder()
                .deploymentIdentifier(this.deploymentIdentifier)
                .context(newContext)
                .configurationTarget(this.configurationTarget)
                .build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionKey that = (ExecutionKey) o;
        return deploymentIdentifier.equals(that.deploymentIdentifier) && context == that.context && configMap.equals(that.configMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deploymentIdentifier, context, configMap);
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private ClassOrMethodIdentifier deploymentIdentifier;
        private Context context;
        private ConfigurationTarget configurationTarget;

        Builder deploymentIdentifier(ClassOrMethodIdentifier deploymentIdentifier) {
            this.deploymentIdentifier = deploymentIdentifier;
            return this;
        }

        Builder context(Context context) {
            this.context = context;
            return this;
        }

        Builder configurationTarget(ConfigurationTarget configurationTarget) {
            this.configurationTarget = configurationTarget;
            return this;
        }

        ExecutionKey build() {
            return new ExecutionKey(
                    ofNullable(deploymentIdentifier).orElseThrow(),
                    ofNullable(context).orElseThrow(),
                    ofNullable(configurationTarget).orElseThrow()
            );
        }
    }
}
