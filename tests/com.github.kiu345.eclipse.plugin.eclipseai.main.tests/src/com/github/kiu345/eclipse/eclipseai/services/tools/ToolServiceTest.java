package com.github.kiu345.eclipse.eclipseai.services.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.github.kiu345.eclipse.eclipseai.services.tools.ToolService.ToolInfo;
import com.github.kiu345.eclipse.util.MockUtils;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

@TestMethodOrder(OrderAnnotation.class)
class ToolServiceTest {

    private static final String TEST_TOOL_NAME = "dateAndTime";

    @Order(1)
    @Test
    void testFindTools() {
        ToolService service = new ToolService();
        service.setToolClasses(SimpleAITools.class);
        List<ToolInfo> tools = service.findTools();

        assertThat(tools)
                .isNotNull()
                .anyMatch(e -> e.getTool() != null && TEST_TOOL_NAME.equals(e.getTool().name()));
    }

    @Order(2)
    @Test
    void testExecuteTool() throws Exception {
        ILog logMock = MockUtils.createLogMock();

        IEclipseContext context = EclipseContextFactory.create();
        context.set(ILog.class, logMock);

        ToolService service = ContextInjectionFactory.make(ToolService.class, context);
        service.setToolClasses(SimpleAITools.class);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name(TEST_TOOL_NAME)
                .arguments("").build();

        final String year = DateTimeFormatter.ofPattern("uuuu").format(LocalDateTime.now());

        List<ToolInfo> tools = service.findTools();
        String result = service.executeTool(tools, request);

        assertThat(result)
                .isNotNull()
                .contains(year);
    }
}
