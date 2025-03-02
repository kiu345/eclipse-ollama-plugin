package com.github.kiu345.eclipse.eclipseai.model;

public record Incoming(Type type, String payload) {
    public enum Type {
        CONTENT,
        FUNCTION_CALL,
        ERROR
    }
}
