package com.github.kiu345.eclipse.assistai.services;

import java.util.List;
import java.util.Optional;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jface.preference.IPreferenceStore;

import com.github.kiu345.eclipse.assistai.Activator;
import com.github.kiu345.eclipse.assistai.model.ModelDescriptor;
import com.github.kiu345.eclipse.assistai.preferences.PreferenceConstants;

import jakarta.inject.Singleton;

@Creatable
@Singleton
public class ClientConfiguration {
    
    public static final boolean DEBUG_MODE=true;
    
    private List<ModelDescriptor> modelList;

    public void setModelList(List<ModelDescriptor> modelList) {
        this.modelList = modelList;
    }

    public Optional<ModelDescriptor> getModelDescriptor(String modelName) {
        return modelList.stream().filter(t -> t.title().equals(modelName)).findFirst();
    }

    public Optional<String> getSelectedModel() {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        return Optional.of(prefernceStore.getString(PreferenceConstants.ASSISTAI_SELECTED_MODEL));
    }

    public void setSelectedModel(String model) {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        prefernceStore.setValue(PreferenceConstants.ASSISTAI_SELECTED_MODEL, model);
    }

    public Optional<Integer> getTemperature() {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        return Optional.of(prefernceStore.getInt(PreferenceConstants.ASSISTAI_TEMPERATURE));
    }

    public void setTemperature(Integer temperature) {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        prefernceStore.setValue(PreferenceConstants.ASSISTAI_TEMPERATURE, temperature);

    }

    public String getApiKey() {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        return prefernceStore.getString(PreferenceConstants.ASSISTAI_API_KEY);
    }

    public String getBaseUrl() {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        return prefernceStore.getString(PreferenceConstants.ASSISTAI_BASE_URL);
    }

    public String getApiBaseUrl() {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        return getBaseUrl() + prefernceStore.getString(PreferenceConstants.ASSISTAI_API_BASE_URL);
    }

    public String getModelApiPath() {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        return getBaseUrl() + prefernceStore.getString(PreferenceConstants.ASSISTAI_GET_MODEL_API_PATH);
    }

    public int getConnectionTimoutSeconds() {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        return Integer.parseInt(prefernceStore.getString(PreferenceConstants.ASSISTAI_CONNECTION_TIMEOUT_SECONDS));
    }

    public int getRequestTimoutSeconds() {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        return Integer.parseInt(prefernceStore.getString(PreferenceConstants.ASSISTAI_REQUEST_TIMEOUT_SECONDS));
    }

    public Optional<String> getConversationId() {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        return Optional.of(prefernceStore.getString(PreferenceConstants.ASSISTAI_CONVERSATION_ID));
    }

    public void setConversationId(String conversationId) {
        IPreferenceStore prefernceStore = Activator.getDefault().getPreferenceStore();
        prefernceStore.setValue(PreferenceConstants.ASSISTAI_CONVERSATION_ID, conversationId);
    }
}
