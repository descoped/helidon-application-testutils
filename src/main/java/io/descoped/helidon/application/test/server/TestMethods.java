package io.descoped.helidon.application.test.server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class TestMethods {

    private final Map<ClassOrMethodIdentifier, TestMethod> testMethodMap = new ConcurrentHashMap<>();
    private final Deployments deployments;
    private final Configurations configurations;

    TestMethods(Deployments deployments, Configurations configurations) {
        this.deployments = deployments;
        this.configurations = configurations;
    }

    void register(TestMethod testMethod) {
        if (testMethodMap.putIfAbsent(testMethod.testIdentifier, testMethod.initialize(deployments, configurations)) != null) {
            throw new IllegalStateException("Already exists: " + testMethod.testIdentifier);
        }
    }
    
    Set<ClassOrMethodIdentifier> identifiers() {
        return testMethodMap.keySet();
    }

    TestMethod tryGet(ClassOrMethodIdentifier testMethodIdentifier) {
        if (!testMethodMap.containsKey(testMethodIdentifier)) {
            throw new IllegalStateException("Not found: " + testMethodIdentifier);
        }

        return testMethodMap.get(testMethodIdentifier);
    }
}
