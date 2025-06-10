package com.github.kiu345.eclipse.eclipseai.services.tools;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import dev.langchain4j.agent.tool.Tool;

public class SimpleAITools {
    @Tool("Returns the current date and time")
    public String dateAndTime() {
        System.out.println("SimpleAITools.dateAndTime()");
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).format(Instant.now());
    }
}
