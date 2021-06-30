package io.descoped.helidon.application.test.scenario;

import io.descoped.helidon.application.base.HelidonDeployment;
import io.descoped.helidon.application.test.server.Deployment;
import io.helidon.config.ConfigSources;

public class SuiteDeployment {

    @Deployment
    static HelidonDeployment createDeployment() {
        return HelidonDeployment.newBuilder()
                .configSource(ConfigSources.classpath("application-test.yaml").optional(true).build())
                .routing(builder -> builder.get("/greet", (req, res) -> res.send("Hello World!")))
                .routing(builder -> builder.get("/greet2", (req, res) -> res.send("Hello there!")))
                .build();
    }

}
