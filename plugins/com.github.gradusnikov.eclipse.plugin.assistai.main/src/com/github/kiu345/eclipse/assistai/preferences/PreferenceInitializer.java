package com.github.kiu345.eclipse.assistai.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.github.kiu345.eclipse.assistai.Activator;
import com.github.kiu345.eclipse.assistai.model.ModelApiDescriptor;
import com.github.kiu345.eclipse.assistai.prompt.PromptLoader;
import com.github.kiu345.eclipse.assistai.prompt.Prompts;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer
{

    public void initializeDefaultPreferences()
    {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault( PreferenceConstants.ASSISTAI_CONNECTION_TIMEOUT_SECONDS, 10 );
        store.setDefault( PreferenceConstants.ASSISTAI_REQUEST_TIMEOUT_SECONDS, 30 );
        
        ModelApiDescriptor llama = new ModelApiDescriptor( "1", "openai", "http://127.0.0.1:11434/api/chat", "ollama", "llama3.2", 7, false, false );
        ModelApiDescriptor codegemma = new ModelApiDescriptor( "2", "openai", "http://127.0.0.1:11434/api/chat", "ollama", "codegemma", 7, false, false );
        String modelsJson = ModelApiDescriptorUtilities.toJson( llama, codegemma );
        store.setDefault( PreferenceConstants.ASSISTAI_SELECTED_MODEL, llama.uid() );
        store.setDefault( PreferenceConstants.ASSISTAI_DEFINED_MODELS, modelsJson );

        PromptLoader promptLoader = new PromptLoader();
        for ( Prompts prompt : Prompts.values() )
        {
            store.setDefault( prompt.preferenceName(), promptLoader.getDefaultPrompt( prompt.getFileName() ) );
        }
    }
}
