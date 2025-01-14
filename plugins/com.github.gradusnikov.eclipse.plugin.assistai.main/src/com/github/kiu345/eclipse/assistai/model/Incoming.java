package com.github.kiu345.eclipse.assistai.model;

public record Incoming( Type type, String payload )
{
    public enum Type
    {
        CONTENT,
        FUNCTION_CALL
    }
}
