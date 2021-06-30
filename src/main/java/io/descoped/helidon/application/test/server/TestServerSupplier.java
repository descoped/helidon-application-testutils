package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.base.HelidonDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

class TestServerSupplier implements Supplier<TestServer> {

    static final Logger LOG = LoggerFactory.getLogger(TestServerSupplier.class);

    private final TestServer.Builder testServerBuilder;
    private final AtomicReference<TestServer> testServerRef = new AtomicReference<>();

    private TestServerSupplier(HelidonDeployment.Builder deploymentBuilder) {
        this.testServerBuilder = new TestServer.Builder().deployment(deploymentBuilder);
    }

    static TestServerSupplier create(HelidonDeployment.Builder deploymentBuilder) {
        return new TestServerSupplier(deploymentBuilder);
    }

    @Override
    public TestServer get() {
        if (!testServerRef.compareAndSet(null, testServerBuilder.build())) { // produce random port
            //LOG.error("The supplier has already been created!\n{}" + StackTraceUtils.printStackTrace());
        }
        return testServerRef.get();
    }

}
