package com.github.kiu345.eclipse.assistai.subscribers;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;

import com.github.kiu345.eclipse.assistai.services.OpenAIStreamJavaHttpClient;

@Creatable
@Singleton
public class OllamaHttpClientProvider {
    @Inject
    private Provider<OpenAIStreamJavaHttpClient> clientProvider;
    @Inject
    private AppendMessageToViewSubscriber appendMessageToViewSubscriber;
    @Inject
    private FunctionCallSubscriber functionCallSubscriber;
    @Inject
    private PrintMessageSubscriber printMessageSubscriber;

    public OpenAIStreamJavaHttpClient get() {
        OpenAIStreamJavaHttpClient client = clientProvider.get();
        client.subscribe(printMessageSubscriber);
        client.subscribe(appendMessageToViewSubscriber);
        client.subscribe(functionCallSubscriber);
        return client;
    }
}
