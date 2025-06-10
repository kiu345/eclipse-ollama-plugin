package com.github.kiu345.eclipse.eclipseai.adapter.github;

import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.github.kiu345.eclipse.eclipseai.adapter.ChatAdapter;
import com.github.kiu345.eclipse.eclipseai.model.ModelDescriptor;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.github.GitHubModelsChatModelName;

public class GithubCopilotAdapter implements ChatAdapter {

    public static final String DEFAULT_URL = "http://localhost:11434";

    private String apiBaseAddress;
    private String apiKey;

    public GithubCopilotAdapter(String apiBaseAddress) {
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
                .allOf(GitHubModelsChatModelName.class)
                .stream()
                .map(e -> new ModelDescriptor(DEFAULT_URL, e.toString(), apiKey, "github", e.name(), true, true))
                .toList();
        return result;
    }

    public StreamingChatModel getChat(String modelName) {
//        return GitHubModelsChatModel().builder()
//                .build();
        throw new NotImplementedException();
    }
}
