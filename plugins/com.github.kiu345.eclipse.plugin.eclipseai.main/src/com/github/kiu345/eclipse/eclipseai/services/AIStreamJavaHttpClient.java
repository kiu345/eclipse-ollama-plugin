package com.github.kiu345.eclipse.eclipseai.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Creatable;

import com.github.kiu345.eclipse.eclipseai.model.ChatMessage;
import com.github.kiu345.eclipse.eclipseai.model.Conversation;
import com.github.kiu345.eclipse.eclipseai.model.Incoming;
import com.github.kiu345.eclipse.eclipseai.model.ModelDescriptor;
import com.github.kiu345.eclipse.eclipseai.services.tools.ToolService;
import com.github.kiu345.eclipse.eclipseai.services.tools.ToolService.ToolInfo;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatRequestParameters;
import jakarta.inject.Inject;

/**
 * A Java HTTP client for streaming requests to OpenAI API.
 * This class allows subscribing to responses received from the OpenAI API and processes the chat completions.
 */
@Creatable
public class AIStreamJavaHttpClient {
    interface Assistant {
        String chat(String message);
    }

    private SubmissionPublisher<Incoming> publisher;

    private Supplier<Boolean> isCancelled = () -> false;

    @Inject
    private ILog logger;

    @Inject
    private ClientConfiguration configuration;

    @Inject
    private ToolService toolService;

    public AIStreamJavaHttpClient() {

        publisher = new SubmissionPublisher<>();
    }

    public void setCancelProvider(Supplier<Boolean> isCancelled) {
        this.isCancelled = isCancelled;
    }

    /**
     * Subscribes a given Flow.Subscriber to receive String data from OpenAI API responses.
     * 
     * @param subscriber the Flow.Subscriber to be subscribed to the publisher
     */
    public synchronized void subscribe(Flow.Subscriber<Incoming> subscriber) {
        publisher.subscribe(subscriber);
    }

    private dev.langchain4j.data.message.ChatMessage toChatMessage(ChatMessage message) {
        switch (message.role) {
            case ChatMessage.ROLE_SYSTEM:
                return new SystemMessage(message.getContent());
            case ChatMessage.ROLE_AI:
                return new AiMessage(message.getContent());
            default:
                return new UserMessage(message.getContent());
        }
    }

    private ChatRequest createRequest(List<dev.langchain4j.data.message.ChatMessage> messages, ModelDescriptor desciption, boolean withFunctions) {
        OllamaChatRequestParameters.Builder paramsBuilder = OllamaChatRequestParameters.builder();
        paramsBuilder.modelName(desciption.model());
        if (withFunctions) {
            paramsBuilder.toolSpecifications(toolService.findTools().stream().map(e -> e.getTool()).toList());
        }

        return ChatRequest.builder()
                .messages(messages)
                .parameters(paramsBuilder.build())
                .build();
    }

    /**
     * Creates and returns a Runnable that will execute the HTTP request to OpenAI API
     * with the given conversation prompt and process the responses.
     * <p>
     * Note: this method does not block and the returned Runnable should be executed
     * to perform the actual HTTP request and processing.
     *
     * @param prompt the conversation to be sent to the OpenAI API
     * @return a Runnable that performs the HTTP request and processes the responses
     */
    public Runnable run(Conversation prompt) {
        return () -> {

            var modelName = configuration.getSelectedModel().orElseThrow();
            var modelInfo = configuration.getModelDescriptor(modelName).orElseThrow();

            ChatModel model = OllamaChatModel.builder()
                    .baseUrl(modelInfo.baseUri())
//                    .apiKey(modelInfo.apiKey())
                    .modelName(modelInfo.model())
                    .build();

            ChatRequest request = createRequest(prompt.messages().stream().map(this::toChatMessage).toList(), modelInfo, configuration.getUseFunctions().orElse(false));
            ChatResponse response = model.doChat(request);
            logger.info("Received response of type " + response.aiMessage().type().name());
//            response.aiMessage().toolExecutionRequests().getFirst().
            while (response.aiMessage().hasToolExecutionRequests()) {
                if (isCancelled.get()) {
                    logger.info("Canceling");
                    break;
                }
                logger.info("Tool exec request detected");
                AiMessage aiMessage = response.aiMessage();

                List<dev.langchain4j.data.message.ChatMessage> allMessages = new ArrayList<dev.langchain4j.data.message.ChatMessage>();
                allMessages.addAll(request.messages());
                allMessages.add(aiMessage);
                
                List<ToolInfo> tools = toolService.findTools();
                for (ToolExecutionRequest execRequest : response.aiMessage().toolExecutionRequests()) {
                    if (isCancelled.get()) {
                        logger.info("Canceling");
                        break;
                    }
                    String toolResultValue = "";
                    
                    try {
                        toolResultValue = toolService.executeTool(tools, execRequest);
                        if (toolResultValue == null) {
                            logger.warn("Tool returned null");
                            toolResultValue = "";
                        }
                    }
                    catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                        continue;
                    }
                    ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(execRequest, toolResultValue);

                    allMessages.add(toolExecutionResultMessage);
                }
                logger.info("resending request");

                request = createRequest(allMessages, modelInfo, configuration.getUseFunctions().orElse(false));
                response = model.doChat(request);
            }

            switch (response.aiMessage().type()) {
                case AI:
                    publisher.submit(new Incoming(Incoming.Type.CONTENT, response.aiMessage().text()));
                    break;
                case SYSTEM:
                    publisher.submit(new Incoming(Incoming.Type.CONTENT, response.aiMessage().text()));
                    break;
                default:
                    publisher.submit(new Incoming(Incoming.Type.CONTENT, response.aiMessage().text()));
                    break;
            }
            publisher.close();
        };
    }

}
