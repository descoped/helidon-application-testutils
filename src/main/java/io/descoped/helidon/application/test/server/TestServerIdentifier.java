package io.descoped.helidon.application.test.server;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class TestServerIdentifier {
    final ExecutionKey executionKey;
    final List<TestMethod> testMethods;

    TestServerIdentifier(ExecutionKey executionKey, List<TestMethod> testMethods) {
        this.executionKey = executionKey;
        this.testMethods = testMethods;
    }

    List<String> getTestClasses() {
        if (executionKey.context == Context.METHOD) {
            throw new IllegalStateException("Unique TestClass resolution can only be provided for Context type ROOT and CLASS!");
        }
        return testMethods.stream()
                .map(m -> m.testIdentifier.className)
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestServerIdentifier that = (TestServerIdentifier) o;
        return executionKey.equals(that.executionKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionKey);
    }
}
