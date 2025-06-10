package com.github.kiu345.eclipse.eclipseai.adapter.local;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.github.kiu345.eclipse.eclipseai.adapter.ChatAdapter;
import com.github.kiu345.eclipse.eclipseai.model.ModelDescriptor;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.localai.LocalAiChatModel;

public class LocalAIAdapter implements ChatAdapter {
    
    public static final String DEFAULT_URL = "http://localhost:8082/v1";
    
    private String apiBaseAddress;
    private String apiKey;

    public LocalAIAdapter(String apiBaseAddress) {
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
        ArrayList<ModelDescriptor> result = new ArrayList<>();
        ModelDescriptor modelDesc = new ModelDescriptor(apiBaseAddress, "gpt-4", apiKey, "local", "gpt-4", true, true);
        result.add(modelDesc);
        return result;
    }

    public ChatModel getChat(String modelName) {
        return LocalAiChatModel.builder()
                .baseUrl(apiBaseAddress)
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true)
                .maxRetries(3)
                .timeout(Duration.ofSeconds(15))
                .build();
    }
}
