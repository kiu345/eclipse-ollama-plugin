package com.github.kiu345.eclipse.eclipseai.services.tools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.github.kiu345.eclipse.eclipseai.util.TextCompareInput;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.inject.Inject;

public class SimpleAITools {
    @Inject
    private ILog log;

    @Tool("Returns the current local date and time")
    public String dateAndTime() {
        System.out.println("SimpleAITools.dateAndTime()");
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(LocalDateTime.now());
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

    @Tool("Returns file content of the editor with the specified name")
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

    @Tool("Opens a new compare editor which shows to text elements to each other. Input needs to be well formatted with whitespaces and newlines.")
    public void showCompareView(
            @P(required = true, value = "The title text of the left side") String leftTitle,
            @P(required = true, value = "The complete text content on the left side. Needs to be the full content, do NOT use segments.") String leftCode,
            @P(required = true, value = "The title text of on the right side") String rightTitle,
            @P(required = true, value = "The complete text content on the right side. Needs to be the full content, do NOT use segments.") String rightCode
    ) {
        IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
        if (windows.length == 0) {
            log.error("no windows found to open view in");
            return;
        }

        IWorkbenchPage page = windows[0].getActivePage();

        CompareConfiguration config = new CompareConfiguration();
        config.setLeftLabel(leftTitle);
        config.setRightLabel(rightTitle);
        config.setLeftEditable(false);
        config.setRightEditable(false);
        config.setProperty(CompareConfiguration.IGNORE_WHITESPACE, true);

        CompareEditorInput input = new CompareEditorInput(config) {

            @Override
            protected Object prepareInput(IProgressMonitor monitor) {
                return new DiffNode(
                        new TextCompareInput(leftCode),
                        new TextCompareInput(rightCode)
                );
            }

            @Override
            public Viewer createDiffViewer(Composite parent) {
                log.info("Craet TextMergeViewer");
                return new TextMergeViewer(parent, config);
            }

        };

        input.setTitle("AI compare");
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                CompareUI.openCompareEditorOnPage(input, page);
            }
        });
    }

}
