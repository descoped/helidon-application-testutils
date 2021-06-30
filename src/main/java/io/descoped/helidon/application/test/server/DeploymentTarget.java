package io.descoped.helidon.application.test.server;

import java.util.Objects;

class DeploymentTarget implements CompareTargetState<DeploymentTarget>, DeploymentLifecycle {

    final ClassOrMethodIdentifier testIdentifier;
    final ClassOrMethodIdentifier deploymentIdentifier;
    private final Context context;

    private DeploymentTarget(ClassOrMethodIdentifier testIdentifier, ClassOrMethodIdentifier deploymentIdentifier) {
        this.testIdentifier = testIdentifier;
        this.deploymentIdentifier = deploymentIdentifier;
        context = testIdentifier.isMethod() ? Context.METHOD : Context.CLASS;
    }

    @Override
    public boolean identicalTo(DeploymentTarget other) {
        return this.deploymentIdentifier.equals(other.deploymentIdentifier);
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeploymentTarget that = (DeploymentTarget) o;
        return testIdentifier.equals(that.testIdentifier) && deploymentIdentifier.equals(that.deploymentIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testIdentifier);
    }

    @Override
    public String toString() {
        return "DeploymentTarget{" +
                "key=" + testIdentifier +
                '}';
    }

    static class Builder {

        private ClassOrMethodIdentifier testIdentifier;
        private ClassOrMethodIdentifier deploymentIdentifier;

        Builder testIdentifier(ClassOrMethodIdentifier testIdentifier) {
            this.testIdentifier = testIdentifier;
            return this;
        }

        Builder deploymentIdentifier(ClassOrMethodIdentifier deploymentIdentifier) {
            this.deploymentIdentifier = deploymentIdentifier;
            return this;
        }

        DeploymentTarget build() {
            return new DeploymentTarget(testIdentifier, deploymentIdentifier);
        }
    }
}
