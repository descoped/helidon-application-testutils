package io.descoped.helidon.application.test.server;

public enum Context {

    ROOT,
    CLASS,
    METHOD;

    static Context ordinalOf(int ordinal) {
        return values()[ordinal];
    }
}
