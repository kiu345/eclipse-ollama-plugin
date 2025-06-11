package com.github.kiu345.eclipse.eclipseai.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jface.preference.IPreferenceStore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kiu345.eclipse.eclipseai.Activator;
import com.github.kiu345.eclipse.eclipseai.commands.FunctionExecutorProvider;
import com.github.kiu345.eclipse.eclipseai.model.ChatMessage;
import com.github.kiu345.eclipse.eclipseai.model.Conversation;
import com.github.kiu345.eclipse.eclipseai.model.Incoming;
import com.github.kiu345.eclipse.eclipseai.model.ModelDescriptor;
import com.github.kiu345.eclipse.eclipseai.part.Attachment;
import com.github.kiu345.eclipse.eclipseai.prompt.Prompts;
import com.github.kiu345.eclipse.eclipseai.services.tools.SimpleAITools;
import com.github.kiu345.eclipse.eclipseai.tools.ImageUtilities;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
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

    private static final String CHAT_API_PATH = "/chat";

    private SubmissionPublisher<Incoming> publisher;

    private Supplier<Boolean> isCancelled = () -> false;

    @Inject
    private ILog logger;

    @Inject
    private ClientConfiguration configuration;

    @Inject
    private FunctionExecutorProvider functionExecutor;

    private IPreferenceStore preferenceStore;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIStreamJavaHttpClient() {

        publisher = new SubmissionPublisher<>();
        preferenceStore = Activator.getDefault().getPreferenceStore();
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

    /**
     * Returns the JSON request body as a String for the given prompt.
     * 
     * @param prompt the user input to be included in the request body
     * @return the JSON request body as a String
     */
    private String getRequestBody(Conversation prompt, ModelDescriptor model) {
        try {

            var requestBody = new LinkedHashMap<String, Object>();
            var messages = new ArrayList<Map<String, Object>>();

            var systemMessage = new LinkedHashMap<String, Object>();
            systemMessage.put("role", "system");
            systemMessage.put("content", preferenceStore.getString(Prompts.SYSTEM.preferenceName()));
            messages.add(systemMessage);

            prompt.messages().stream().forEach(e -> e.setContent(e.getContent().trim()));

            prompt.messages().stream().filter(e -> !e.getContent().isBlank()).map(message -> toJsonPayload(message, model)).forEach(messages::add);

            requestBody.put("model", model.model());
            if (model.functionCalling() && false) {
                requestBody.put("functions", AnnotationToJsonConverter.convertDeclaredFunctionsToJson(functionExecutor.get().getFunctions()));
            }
            requestBody.put("messages", messages);

            float temperature = configuration.getTemperature().orElse(5);
            requestBody.put("temperature", temperature / 10f);
            requestBody.put("stream", true);

            String jsonString;
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
            return jsonString;
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private LinkedHashMap<String, Object> toJsonPayload(ChatMessage message, ModelDescriptor model) {
        try {
            var userMessage = new LinkedHashMap<String, Object>();
            userMessage.put("role", message.getRole());
            if (model.functionCalling() && false) {
                // function call results
                if (Objects.nonNull(message.getName())) {
                    userMessage.put("name", message.getName());
                }
                if (Objects.nonNull(message.getFunctionCall())) {
                    var functionCallObject = new LinkedHashMap<String, String>();
                    functionCallObject.put("name", message.getFunctionCall().name());
                    functionCallObject.put("arguments", objectMapper.writeValueAsString(message.getFunctionCall().arguments()));
                    userMessage.put("function_call", functionCallObject);
                }
            }

            // assemble text content
            List<String> textParts = message.getAttachments()
                    .stream()
                    .map(Attachment::toChatMessageContent)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            String textContent = String.join("\n", textParts) + "\n\n" + message.getContent();

            // add image content
            if (model.vision() && false) {
                var content = new ArrayList<>();
                var textObject = new LinkedHashMap<String, String>();
                textObject.put("type", "text");
                textObject.put("text", textContent);
                content.add(textObject);
                message.getAttachments()
                        .stream()
                        .map(Attachment::getImageData)
                        .filter(Objects::nonNull)
                        .map(ImageUtilities::toBase64Jpeg)
                        .map(this::toImageUrl)
                        .forEachOrdered(content::add);
                userMessage.put("content", content);
            }
            else // legacy API - just put content as text
            {
                userMessage.put("content", textContent);
            }
            return userMessage;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Converts a base64-encoded image data string into a structured JSON object suitable for API transmission.
     * <p>
     * This method constructs a JSON object that encapsulates the image data in a format expected by the API.
     * The 'image_url' key is an object containing a 'url' key, which holds the base64-encoded image data prefixed
     * with the appropriate data URI scheme.
     *
     * @param data the base64-encoded string of the image data
     * @return a LinkedHashMap where the key 'type' is set to 'image_url', and 'image_url' is another LinkedHashMap
     *         containing the 'url' key with the full data URI of the image.
     */
    private LinkedHashMap<String, Object> toImageUrl(String data) {
        var imageObject = new LinkedHashMap<String, Object>();
        imageObject.put("type", "image_url");
        var urlObject = new LinkedHashMap<String, String>();
        urlObject.put("url", "data:image/jpeg;base64," + data);
        imageObject.put("image_url", urlObject);
        return imageObject;
    }

    private List<ToolSpecification> getTools() {
        List<ToolSpecification> result = new LinkedList<>();
        result.addAll(ToolSpecifications.toolSpecificationsFrom(SimpleAITools.class));
//        System.out.println(Arrays.toString(result.toArray()));
        return result;
    }

    private ChatRequest createRequest(List<dev.langchain4j.data.message.ChatMessage> messages, ModelDescriptor desciption, boolean withFunctions) {
        OllamaChatRequestParameters.Builder paramsBuilder = OllamaChatRequestParameters.builder();
        paramsBuilder.modelName(desciption.model());
        if (withFunctions) {
            paramsBuilder.toolSpecifications(getTools());
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
            ChatResponse response = model.chat(request);
            logger.info("Received response of type "+response.aiMessage().type().name());
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

//            String answer = assistant.chat("Hi");
//            publisher.submit(new Incoming(Incoming.Type.CONTENT, answer));

            /*
             * final OllamaStreamingChatModel chatModel = new OllamaAdapter(configuration).getChat(model.model());
             * chatModel.doChat(createRequest(prompt.messages().stream().map(this::toChatMessage).toList(), model), new StreamingChatResponseHandler() {
             * private StringBuilder builder = new StringBuilder();
             * 
             * 
             * @Override
             * public void onPartialResponse(String partialResponse) {
             * builder.append(partialResponse);
             * }
             * 
             * @Override
             * public void onCompleteResponse(ChatResponse completeResponse) {
             * ChatMessageType type = completeResponse.aiMessage().type();
             * logger.info(type.name());
             * switch (type) {
             * case AI: {
             * for (ToolExecutionRequest execRequest: completeResponse.aiMessage().toolExecutionRequests()) {
             * }
             * publisher.submit(new Incoming(Incoming.Type.CONTENT, completeResponse.aiMessage().text()));
             * break;
             * }
             * case CUSTOM: {
             * publisher.submit(new Incoming(Incoming.Type.CONTENT, completeResponse.aiMessage().text()));
             * break;
             * }
             * case SYSTEM: {
             * publisher.submit(new Incoming(Incoming.Type.CONTENT, completeResponse.aiMessage().text()));
             * break;
             * }
             * case TOOL_EXECUTION_RESULT: {
             * publisher.submit(new Incoming(Incoming.Type.CONTENT, completeResponse.aiMessage().text()));
             * break;
             * }
             * case USER: {
             * publisher.submit(new Incoming(Incoming.Type.CONTENT, completeResponse.aiMessage().text()));
             * break;
             * }
             * default:
             * throw new IllegalArgumentException("Unexpected value: " + type);
             * }
             * publisher.close();
             * logger.info(completeResponse.finishReason().name());
             * }
             * 
             * @Override
             * public void onError(Throwable error) {
             * logger.error(error.getMessage(), error);
             * publisher.closeExceptionally(error);
             * }
             * 
             * });
             * 
             */
        };
    }

}
