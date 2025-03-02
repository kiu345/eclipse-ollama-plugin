package com.github.kiu345.eclipse.eclipseai.handlers;

import com.github.kiu345.eclipse.eclipseai.prompt.Prompts;

public class AssistAIUnitTestHandler extends AssistAIHandlerTemplate {

    public AssistAIUnitTestHandler() {
        super(Prompts.TEST_CASE);
    }
}
