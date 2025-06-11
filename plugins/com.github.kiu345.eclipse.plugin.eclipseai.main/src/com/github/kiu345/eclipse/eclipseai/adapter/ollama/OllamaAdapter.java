package com.github.kiu345.eclipse.eclipseai.adapter.ollama;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.github.kiu345.eclipse.eclipseai.adapter.ChatAdapter;
import com.github.kiu345.eclipse.eclipseai.model.ModelDescriptor;
import com.github.kiu345.eclipse.eclipseai.services.ClientConfiguration;

import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.ollama.OllamaModel;
import dev.langchain4j.model.ollama.OllamaModels;
import dev.langchain4j.model.ollama.OllamaModels.OllamaModelsBuilder;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.output.Response;

public class OllamaAdapter implements ChatAdapter {

    public static final String DEFAULT_URL = "http://localhost:11434";

    private ClientConfiguration clientConfig;

    public OllamaAdapter(ClientConfiguration clientConfig) {
        this.clientConfig = clientConfig;
    }

    public List<ModelDescriptor> getModels() {
        ArrayList<ModelDescriptor> result = new ArrayList<>();

        OllamaModels modelsService = new OllamaModelsBuilder()
                .httpClientBuilder(new JdkHttpClientBuilder())
                .baseUrl(clientConfig.getBaseUrl())
                .timeout(Duration.ofSeconds(30))
                .maxRetries(3)
                .logRequests(true)
                .logResponses(true)
                .build();

        try {
            Response<List<OllamaModel>> response = modelsService.availableModels();
            List<OllamaModel> models = response.content();
            if (models != null) {
                for (OllamaModel model : models) {
                    ModelDescriptor modelDesc = new ModelDescriptor(clientConfig.getBaseUrl(), model.getName(), clientConfig.getApiKey(), "ollama", model.getName(), true, true);
                    result.add(modelDesc);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public OllamaStreamingChatModel getChat(String modelName) {
        List<ChatModelListener> listeners = new ArrayList<ChatModelListener>();
        listeners.add(new ChatModelListener() {
            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                System.out.println("Got response:");
                System.out.println(responseContext.chatResponse().aiMessage().type());
            }

            @Override
            public void onError(ChatModelErrorContext errorContext) {
                ChatModelListener.super.onError(errorContext);
            }
        });

        return OllamaStreamingChatModel.builder()
                .baseUrl(clientConfig.getBaseUrl())
                .httpClientBuilder(new JdkHttpClientBuilder())
                .modelName(modelName)
                .listeners(listeners)
                .logRequests(true)
                .logResponses(true)
                .temperature(clientConfig.getTemperature().orElse(4) / 10d)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
