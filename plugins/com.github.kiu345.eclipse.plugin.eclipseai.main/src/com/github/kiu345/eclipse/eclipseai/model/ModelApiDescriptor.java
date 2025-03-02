package com.github.kiu345.eclipse.eclipseai.model;

/**
 * 
 */
public record ModelApiDescriptor(
        String uid,
        String apiType,
        String apiUrl,
        String apiKey,
        String modelName,
        int temperature,
        boolean vision,
        boolean functionCalling) {}
