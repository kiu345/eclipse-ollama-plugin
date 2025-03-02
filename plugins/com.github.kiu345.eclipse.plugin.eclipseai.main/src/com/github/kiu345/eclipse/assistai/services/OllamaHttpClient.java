package com.github.kiu345.eclipse.assistai.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Creatable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kiu345.eclipse.assistai.model.ModelDescriptor;

import jakarta.inject.Inject;

@Creatable
public class OllamaHttpClient {

    @Inject
    private ILog logger;

    @Inject
    private ClientConfiguration configuration;

    private HttpClient getHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(configuration.getConnectionTimoutSeconds())).build();
    }

    public List<ModelDescriptor> getModelList() {
        HttpClient httpClient = getHttpClient();

        String modelApiUrl = configuration.getModelApiPath();

        List<ModelDescriptor> models = new ArrayList<>();

        try {
            URI uri = URI.create(modelApiUrl);
            logger.info(uri.toASCIIString());
            HttpRequest httpRequest = HttpRequest.newBuilder().uri(uri)
                    .timeout(Duration.ofSeconds(configuration.getRequestTimoutSeconds()))
                    .version(HttpClient.Version.HTTP_1_1).header("Authorization", configuration.getApiKey())
                    .header("Content-Type", "application/json").build();

            HttpResponse<String> response = httpClient.send(httpRequest, BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();

            JsonNode rootNode = mapper.readTree(response.body());
            JsonNode modelListNode = rootNode.get("models");

            for (int i = 0; i < modelListNode.size(); i++) {
                JsonNode modelNode = modelListNode.get(i);
                if (modelNode == null)
                    continue;
                logger.info(modelNode.toString());

                models.add(
                        new ModelDescriptor(
                                modelNode.get("apiBase") == null || modelNode.get("apiBase").asText().isEmpty()
                                        ? configuration.getApiBaseUrl()
                                        : modelNode.get("apiBase").asText(),
                                modelNode.get("model").asText(), configuration.getApiKey(),
                                "ollama", modelNode.get("name").asText(), false, false
                        )
                );
            }
        }
        catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
        catch (InterruptedException e) {
            logger.warn(e.getMessage(), e);
        }
        catch (IllegalArgumentException e) {
            logger.warn(e.getMessage(), e);
        }

        return models;
    }
}
