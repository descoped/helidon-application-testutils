package io.descoped.helidon.application.test.server;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;

import java.lang.reflect.Method;
import java.util.Objects;

class ClassOrMethodIdentifier {

    final String className;
    final String methodName;

    private ClassOrMethodIdentifier(String className) {
        Objects.requireNonNull(className);
        this.className = className;
        this.methodName = null;
    }

    private ClassOrMethodIdentifier(String className, String methodName) {
        Objects.requireNonNull(className);
        Objects.requireNonNull(methodName);
        this.className = className;
        this.methodName = methodName;
    }

    static ClassOrMethodIdentifier from(ExtensionContext extensionContext) {
        Objects.requireNonNull(extensionContext);
        return extensionContext.getTestMethod().isPresent() ?
            ClassOrMethodIdentifier.from(extensionContext.getRequiredTestClass(), extensionContext.getRequiredTestMethod()) :
            ClassOrMethodIdentifier.from(extensionContext.getRequiredTestClass());
    }

    static ClassOrMethodIdentifier from(TestSource testSource) {
        Objects.requireNonNull(testSource);
        if (testSource instanceof ClassSource) {
            return new ClassOrMethodIdentifier(((ClassSource) testSource).getClassName());

        } else if (testSource instanceof MethodSource) {
            return new ClassOrMethodIdentifier(((MethodSource) testSource).getClassName(), ((MethodSource) testSource).getMethodName());

        } else {
            throw new IllegalStateException();
        }
    }

    static ClassOrMethodIdentifier from(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        return new ClassOrMethodIdentifier(clazz.getName());
    }

    static ClassOrMethodIdentifier from(Class<?> clazz, Method method) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(method);
        return new ClassOrMethodIdentifier(clazz.getName(), method.getName());
    }

    static ClassOrMethodIdentifier from(String className) {
        Objects.requireNonNull(className);
        return new ClassOrMethodIdentifier(className);
    }

    static ClassOrMethodIdentifier from(Class<?> clazz, String methodName) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(methodName);
        return new ClassOrMethodIdentifier(clazz.getName(), methodName);
    }

    static ClassOrMethodIdentifier from(String className, String methodName) {
        Objects.requireNonNull(className);
        Objects.requireNonNull(methodName);
        return new ClassOrMethodIdentifier(className, methodName);
    }

    boolean isClass() {
        return methodName == null;
    }

    boolean isMethod() {
        return methodName != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassOrMethodIdentifier that = (ClassOrMethodIdentifier) o;
        return className.equals(that.className) && Objects.equals(methodName, that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName);
    }

    @Override
    public String toString() {
        return "ClassOrMethodIdentifier{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
