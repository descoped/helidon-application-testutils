package io.descoped.helidon.application.test.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Deployments {

    private final Map<ClassOrMethodIdentifier, DeploymentTarget> deploymentTargetMap = new ConcurrentHashMap<>();

    void register(DeploymentTarget deploymentTarget) {
        if (deploymentTargetMap.putIfAbsent(deploymentTarget.testIdentifier, deploymentTarget) != null) {
            throw new IllegalStateException(deploymentTarget.testIdentifier + " already exists!");
        }
    }

    /**
     * Try DeploymentTarget will try to retrieve applied testIdentifier and if not present it will fallback
     * and try to obtain by Class level identifier..
     *
     * @param testIdentifier
     * @return
     */
    DeploymentTarget tryGet(ClassOrMethodIdentifier testIdentifier) {
        // method match
        if (deploymentTargetMap.containsKey(testIdentifier)) {
            return deploymentTargetMap.get(testIdentifier);
        }
        // try class match
        return deploymentTargetMap.get(ClassOrMethodIdentifier.from(testIdentifier.className));
    }
}
