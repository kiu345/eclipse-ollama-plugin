package com.github.kiu345.eclipse.eclipseai.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.github.kiu345.eclipse.eclipseai.part.ChatPresenter;
import com.github.kiu345.eclipse.eclipseai.prompt.ChatMessageFactory;
import com.github.kiu345.eclipse.eclipseai.prompt.Prompts;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class AssistAIJavaUpgradeHandler {
    @Inject
    private ILog logger;
    @Inject
    private ChatMessageFactory chatMessageFactory;
    @Inject
    private ChatPresenter viewPresenter;

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell s) {
        runPrompt();
    }

    public void runPrompt() {
        // Get the active workbench window
        var workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        // Get the active editor's input file
        var activeEditor = workbenchWindow.getActivePage().getActiveEditor();

        var activeFile = activeEditor.getEditorInput().getName();
        var fileContents = "";

        // Read the content from the file
        // this fixes skipped empty lines issue
        IFile file = (IFile) activeEditor.getEditorInput().getAdapter(IFile.class);
        try {
            fileContents = new String(Files.readAllBytes(file.getLocation().toFile().toPath()), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        var filePath = file.getProjectRelativePath().toString(); // use project relative path
        var ext = activeFile.substring(activeFile.lastIndexOf(".") + 1);

        logger.info("filePath = " + filePath);

        var context = new Context(filePath, fileContents, "", "", "", ext, 0, 0);
        var message = chatMessageFactory.createUserChatMessage(Prompts.UPGRADE_SOURCE, context);
        viewPresenter.onSendPredefinedPrompt(Prompts.UPGRADE_SOURCE, message);
    }
}
