package com.github.kiu345.eclipse.assistai.model;

public record ModelDescriptor(
        String baseUri,
        String model,
        String apiKey,
        String providfer,
        String title,
        boolean vision,
        boolean functionCalling) {}
