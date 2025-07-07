package com.github.kiu345.eclipse.eclipseai.adapter.openai;

import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.github.kiu345.eclipse.eclipseai.adapter.ChatAdapter;
import com.github.kiu345.eclipse.eclipseai.model.ModelDescriptor;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;

public class OpenAIAdapter implements ChatAdapter {

    public static final String DEFAULT_URL = "https://api.openai.com";

    private String apiBaseAddress;
    private String apiKey;

    public OpenAIAdapter(String apiBaseAddress) {
        this.apiBaseAddress = apiBaseAddress;
    }

    public void setApiBaseAddress(String apiBaseAddress) {
        if (apiBaseAddress == null) {
            throw new NullPointerException("Null not allowed for parameter apiBaseAddress");
        }
        this.apiBaseAddress = apiBaseAddress;
    }

    public String getApiBaseAddress() {
        return apiBaseAddress;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public List<ModelDescriptor> getModels() {
        List<ModelDescriptor> result = EnumSet
                .allOf(OpenAiChatModelName.class)
                .stream()
                .map(e -> new ModelDescriptor(DEFAULT_URL, e.toString(), apiKey, "openai", e.name(), true, true, false))
                .toList();
        return result;
    }

    public ChatModel getChat(String modelName) {
//        return new OllamaChatModelBuilder()
//                .baseUrl(apiBaseAddress)
//                .httpClientBuilder(new JdkHttpClientBuilder())
//                .modelName(modelName)
//                .logRequests(true)
//                .logResponses(true)
//                .maxRetries(3)
//                .timeout(Duration.ofSeconds(15))
//                .build();
        throw new NotImplementedException();
    }
}
