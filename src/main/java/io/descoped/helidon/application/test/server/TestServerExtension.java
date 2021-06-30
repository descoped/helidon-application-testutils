package io.descoped.helidon.application.test.server;

import io.descoped.helidon.application.test.client.TestClient;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Optional.ofNullable;

public class TestServerExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver, AfterTestExecutionCallback {

    static final Logger LOG = LoggerFactory.getLogger(TestServerExtension.class);

    private final TestServerFactory testServerFactory;

    public TestServerExtension() {
        testServerFactory = TestServerFactory.instance();
    }

    public TestServerExtension(TestServerFactory factory) {
        testServerFactory = factory;
    }

    private ExtensionContext.Store getStore(ExtensionContext extensionContext, Object namespace, Context context) {
        ExtensionContext.Store store;
        if (context == Context.ROOT) {
            store = extensionContext.getRoot().getStore(ExtensionContext.Namespace.create(namespace));

        } else if (context == Context.CLASS) {
            store = extensionContext.getParent().map(parent -> parent.getStore(ExtensionContext.Namespace.create(namespace))).orElseThrow();

        } else if (context == Context.METHOD) {
            store = extensionContext.getStore(ExtensionContext.Namespace.create(namespace));

        } else {
            throw new IllegalStateException();
        }
        return store;
    }

//    private TestServerResource getTestServerResource(ExtensionContext context, TestScopeBinding fallbackTestScopeBinding) {
//        ExtensionContext.Store store = getStore(context, fallbackTestScopeBinding);
//        return store.get(fallbackTestScopeBinding, TestServerResource.class);
//    }

    boolean injectFieldValue(Field field, Object instance, Object value) {
        if (field.isAnnotationPresent(Inject.class) && value.getClass().isAssignableFrom(field.getType())) {
            try {
                if (ModifierSupport.isFinal(field)) {
                    throw new IllegalAccessException("Illegal access to final field: " + instance.getClass().getName() + "." + field.getName());
                }

                ReflectionUtils.makeAccessible(field).set(instance, value);

                return true;

            } catch (IllegalArgumentException e) {
                throw e;
            } catch (IllegalAccessException e) {
                throw new TestServerException(e);
            }
        }
        return false;
    }

    private TestServerResource createOrGetTestServerResource(ExtensionContext extensionContext, ClassOrMethodIdentifier testIdentifier, TestServerIdentifier testServerIdentifier, TestServerSupplier supplier) {
        ExtensionContext.Store store = getStore(extensionContext, testIdentifier, testServerIdentifier.executionKey.context);

        // cleanup previous method if exists
        if (testIdentifier.isMethod()) {
            store.remove(testIdentifier, TestServerResource.class);
        }

        // produce resource
        TestServerResource testServerResource = store.getOrComputeIfAbsent(
                testIdentifier,
                produce -> {
                    return new TestServerResource(testServerIdentifier, testIdentifier, supplier.get()); // produce random port
                },
                TestServerResource.class
        );
        testServerFactory.setCurrentTestServerResource(testServerResource);
        return testServerResource;
    }

    private boolean isRootContext(ExtensionContext context) {
        return context.getTestInstance().isEmpty();
    }

    private void computeAndStartTestServerIfInactive(ExtensionContext extensionContext, ClassOrMethodIdentifier testIdentifier, TestServerIdentifier testServerIdentifier, TestServerSupplier supplier) {
        TestServerResource testServerResource = createOrGetTestServerResource(extensionContext, testIdentifier, testServerIdentifier, supplier);
        testServerResource.startIfInactive();

        if (isRootContext(extensionContext)) {
            return;
        }

        Object testInstance = extensionContext.getRequiredTestInstance();
        List<Field> injectFields = ReflectionSupport.findFields(extensionContext.getRequiredTestClass(),
                field -> field.isAnnotationPresent(Inject.class), HierarchyTraversalMode.TOP_DOWN);

        for (Field field : injectFields) {
            // test server
            injectFieldValue(field, testInstance, testServerResource.getServer());

            // test client
            injectFieldValue(field, testInstance, testServerResource.getClient());
        }
    }

    // skip handling when used with @RegisterExtension annotation
    private boolean isNotValidTestServerExtension(ExtensionContext extensionContext) {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        if (!testClass.isAnnotationPresent(ExtendWith.class)) {
            return true;
        }
        ExtendWith extendWith = testClass.getAnnotation(ExtendWith.class);
        return Arrays.stream(extendWith.value()).noneMatch(clazz -> clazz.isAssignableFrom(TestServerExtension.class));
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (isNotValidTestServerExtension(extensionContext)) {
            return;
        }

        if (testServerFactory == null) {
            throw new IllegalStateException("Deployment NOT found!");
        }

        // find TestServerSupplier for both ROOT and CLASS context
        ClassOrMethodIdentifier testClassIdentifier = ClassOrMethodIdentifier.from(extensionContext.getRequiredTestClass());
        Map<TestServerIdentifier, TestServerSupplier> testServerTargets = testServerFactory.findTestServerSuppliersForRootOrClassContextByClassIdentifier(testClassIdentifier);
        for (Map.Entry<TestServerIdentifier, TestServerSupplier> entry : testServerTargets.entrySet()) {
            LOG.debug("BEGIN {} @ Context: {}", extensionContext.getRequiredTestClass().getSimpleName(), entry.getKey().executionKey.context);
            computeAndStartTestServerIfInactive(extensionContext, testClassIdentifier, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        if (isNotValidTestServerExtension(extensionContext)) {
            return;
        }

        if (extensionContext.getTestInstance().isEmpty()) {
            return;
        }

        ClassOrMethodIdentifier testMethodIdentifier = ClassOrMethodIdentifier.from(extensionContext.getRequiredTestClass(), extensionContext.getRequiredTestMethod());
        Map.Entry<TestServerIdentifier, TestServerSupplier> entry = testServerFactory.findTestServerSuppliersForMethodContextByMethodIdentifier(testMethodIdentifier);
        LOG.debug("BEGIN {} # {} @ Context: {}", testMethodIdentifier.className, testMethodIdentifier.methodName, entry.getKey().executionKey.context);
        ClassOrMethodIdentifier testIdentifier = entry.getKey().executionKey.context == Context.METHOD ? testMethodIdentifier : ClassOrMethodIdentifier.from(testMethodIdentifier.className);
        computeAndStartTestServerIfInactive(extensionContext, testIdentifier, entry.getKey(), entry.getValue());
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        if (isNotValidTestServerExtension(extensionContext)) {
            return;
        }
        ClassOrMethodIdentifier testMethodIdentifier = ClassOrMethodIdentifier.from(extensionContext.getRequiredTestClass(), extensionContext.getRequiredTestMethod());
        Map.Entry<TestServerIdentifier, TestServerSupplier> entry = testServerFactory.findTestServerSuppliersForMethodContextByMethodIdentifier(testMethodIdentifier);
        LOG.debug("END {} # {} @ Context: {}", testMethodIdentifier.className, testMethodIdentifier.methodName, entry.getKey().executionKey.context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return List.of(TestServer.class, TestClient.class).stream().anyMatch(paramClass -> parameterContext.getParameter().getType().equals(paramClass));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ClassOrMethodIdentifier testMethodIdentifier = ClassOrMethodIdentifier.from(extensionContext.getRequiredTestClass(), extensionContext.getRequiredTestMethod());
        TestServerIdentifier testServerIdentifier = testServerFactory.findTestServerSuppliersForMethodContextByMethodIdentifier(testMethodIdentifier).getKey();
        ClassOrMethodIdentifier testIdentifier = testServerIdentifier.executionKey.context == Context.METHOD ? testMethodIdentifier : ClassOrMethodIdentifier.from(testMethodIdentifier.className);
        ExtensionContext.Store store = getStore(extensionContext, testIdentifier, testServerIdentifier.executionKey.context);
        TestServerResource testServerResource = store.get(testIdentifier, TestServerResource.class);

        Class<?> parameterType = parameterContext.getParameter().getType();
        if (parameterType.equals(TestServer.class)) {
            return testServerResource.getServer();

        } else if (parameterType.equals(TestClient.class)) {
            return testServerResource.getClient();

        } else {
            throw new UnsupportedOperationException("Parameter type '" + parameterType.getName() + "' is NOT implemented!");
        }
    }

    public static class TestServerResource implements ExtensionContext.Store.CloseableResource {

        private final TestServerIdentifier testServerIdentifier;
        private final ClassOrMethodIdentifier testIdentifier;
        private final TestServer testServer;
        private final TestClient testClient;
        private final AtomicBoolean closed = new AtomicBoolean();

        public TestServerResource(TestServerIdentifier testServerIdentifier, ClassOrMethodIdentifier testIdentifier, TestServer testServer) {
            this.testServerIdentifier = testServerIdentifier;
            this.testIdentifier = testIdentifier;
            this.testServer = testServer;
            this.testClient = TestClient.create(testServer);
        }

        public TestServer getServer() {
            return testServer;
        }

        public TestClient getClient() {
            return testClient;
        }

        public void startIfInactive() {
            if (closed.get()) {
                return;
            }
            testServer.start()
                    .toCompletableFuture()
                    .orTimeout(30, TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        LOG.error("While starting application", throwable);
                        System.exit(1);
                        return null;
                    });

        }

        public void stop() {
            if (closed.compareAndSet(false, true))
                testServer.stop()
                        .toCompletableFuture()
                        .orTimeout(30, TimeUnit.SECONDS)
                        .exceptionally(throwable -> {
                            LOG.error("While shutting down application", throwable);
                            System.exit(1);
                            return null;
                        });
        }

        @Override
        public void close() {
            if (closed.get()) {
                return;
            }
            if (false) {
                LOG.trace("STOP {}{} @ Context: {} # WebServer at {}://{}:{}",
                        testIdentifier.className,
                        ofNullable(testIdentifier.methodName).map(s -> " # " + s).orElse(""),
                        testServerIdentifier.executionKey.context,
                        testServer.getTestServerProtocol(),
                        testServer.getTestServerHost(),
                        testServer.getTestServerServicePort());
            }
            stop();
        }
    }
}
