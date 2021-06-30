package io.descoped.helidon.application.test.server;

import java.util.Objects;

import static java.util.Optional.ofNullable;

class TestMethod implements CompareTargetState<TestMethod>, DeploymentLifecycle {

    final ClassOrMethodIdentifier testIdentifier;
    private DeploymentTarget deploymentTarget;
    private ConfigurationTarget configurationTarget;
    private Context context;

    TestMethod(ClassOrMethodIdentifier testIdentifier) {
        if (!testIdentifier.isMethod()) {
            throw new IllegalArgumentException("ClassOrMethodIdentifier MUST BE a method level identifier. Illegal argument: " + testIdentifier);
        }
        this.testIdentifier = testIdentifier;
    }

    TestMethod initialize(Deployments deployments, Configurations configurations) {
        deploymentTarget = ofNullable(deployments.tryGet(testIdentifier)).orElseThrow(() -> new IllegalStateException("Deployment NOT found for: " + testIdentifier));
        configurationTarget = ofNullable(configurations.tryGet(testIdentifier)).orElseThrow(() -> new IllegalStateException("Configuration NOT found for: " + testIdentifier));

        Context deploymentContext = deploymentTarget.context();
        Context configurationContext = configurationTarget.context();
        int targetConfigurationSize = configurationTarget.configurationList.size();

        if (deploymentContext == Context.METHOD || (configurationContext == Context.METHOD && targetConfigurationSize > 0)) {
            context = Context.METHOD;
        } else {
            context = Context.CLASS;
        }
        return this;
    }

    private void checkIfInitialized() {
        Objects.requireNonNull(deploymentTarget, "Deployments MUST be set!");
        Objects.requireNonNull(configurationTarget, "Configurations MUST be set!");
    }

    DeploymentTarget deploymentTarget() {
        checkIfInitialized();
        return deploymentTarget;
    }

    ConfigurationTarget configurationTarget() {
        checkIfInitialized();
        return configurationTarget;
    }

    @Override
    public boolean identicalTo(TestMethod other) {
        checkIfInitialized();
        boolean compareDeploymentResult = this.deploymentTarget().identicalTo(other.deploymentTarget());
        boolean compareConfigurationResult = this.configurationTarget().identicalTo(other.configurationTarget());
        return compareDeploymentResult && compareConfigurationResult;
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestMethod that = (TestMethod) o;
        return testIdentifier.equals(that.testIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testIdentifier);
    }

    @Override
    public String toString() {
        return "TestMethod{" +
                "testIdentifier=" + testIdentifier +
                '}';
    }
}
