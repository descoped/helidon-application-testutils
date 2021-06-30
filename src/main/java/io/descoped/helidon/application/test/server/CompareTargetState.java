package io.descoped.helidon.application.test.server;

public interface CompareTargetState<T> {

    boolean identicalTo(T other);

}
