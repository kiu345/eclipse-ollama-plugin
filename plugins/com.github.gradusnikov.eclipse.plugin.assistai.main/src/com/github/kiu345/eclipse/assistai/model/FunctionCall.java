package com.github.kiu345.eclipse.assistai.model;

import java.util.Map;

public record FunctionCall( String name, Map<String, String> arguments  ) {}
