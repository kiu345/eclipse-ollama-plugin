package com.github.kiu345.eclipse.assistai.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.github.kiu345.eclipse.assistai.Activator;
import com.github.kiu345.eclipse.assistai.prompt.PromptLoader;
import com.github.kiu345.eclipse.assistai.prompt.Prompts;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(PreferenceConstants.ASSISTAI_CONNECTION_TIMEOUT_SECONDS, 10);
        store.setDefault(PreferenceConstants.ASSISTAI_REQUEST_TIMEOUT_SECONDS, 30);

        store.setDefault(PreferenceConstants.ASSISTAI_BASE_URL, "http://localhost:11434");
        store.setDefault(PreferenceConstants.ASSISTAI_API_BASE_URL, "/api");
        store.setDefault(PreferenceConstants.ASSISTAI_GET_MODEL_API_PATH, "/api/tags");
        store.setDefault(PreferenceConstants.ASSISTAI_API_KEY, "ollama");

        PromptLoader promptLoader = new PromptLoader();
        for (Prompts prompt : Prompts.values()) {
            store.setDefault(prompt.preferenceName(), promptLoader.getDefaultPrompt(prompt.getFileName()));
        }
    }
}
