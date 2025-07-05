package com.github.kiu345.eclipse.eclipseai.services.tools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.inject.Inject;

public class SimpleAITools {
    @Inject
    private ILog log;

    @Tool("Returns the current date and time")
    public String dateAndTime() {
        System.out.println("SimpleAITools.dateAndTime()");
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(LocalDateTime.now());
    }

    @Tool("Returns the files that are opened in the IDE")
    public String[] openEditors() {
        ArrayList<String> editorTitles = new ArrayList<String>();
        IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
        for (IWorkbenchWindow window : windows) {
            IWorkbenchPage[] pages = window.getPages();
            for (IWorkbenchPage page : pages) {
                IEditorReference[] editors = page.getEditorReferences();
                for (IEditorReference editor : editors) {
                    editorTitles.add(editor.getName());
                }
            }
        }
        return editorTitles.toArray(new String[] {});
    }

    @Tool("Returns active editor name")
    public String activeEditor() {
        IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
        for (IWorkbenchWindow window : windows) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IEditorPart editor = page.getActiveEditor();
                if (editor != null) {
                    return editor.getEditorInput().getName();
                }
                else {
                    log.warn("no active editor");
                }

            }
            else {
                log.warn("no active page");
            }
        }
        return "";
    }

    @Tool("Returns file content of the editor")
    public String editorContent(@P(required = true, value = "The editor name") String name) {
        IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
        for (IWorkbenchWindow window : windows) {
            IWorkbenchPage[] pages = window.getPages();
            for (IWorkbenchPage page : pages) {
                IEditorReference[] editors = page.getEditorReferences();
                for (IEditorReference editor : editors) {
                    if (name.equals(editor.getName())) {
                        IEditorPart editorInstance = editor.getEditor(true);
                        if (editorInstance != null) {
                            IEditorInput input = editorInstance.getEditorInput();
                            if (input instanceof FileEditorInput) {
                                IFile file = ((FileEditorInput) input).getFile();
                                try {
                                    return file.readString();
                                }
                                catch (CoreException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
