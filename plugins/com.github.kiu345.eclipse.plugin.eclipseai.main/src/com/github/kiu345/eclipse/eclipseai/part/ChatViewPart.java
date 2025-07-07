package com.github.kiu345.eclipse.eclipseai.part;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.kiu345.eclipse.eclipseai.model.ChatMessage;
import com.github.kiu345.eclipse.eclipseai.model.ModelDescriptor;
import com.github.kiu345.eclipse.eclipseai.part.Attachment.FileContentAttachment;
import com.github.kiu345.eclipse.eclipseai.part.dnd.DropManager;
import com.github.kiu345.eclipse.eclipseai.prompt.InputParser;
import com.github.kiu345.eclipse.eclipseai.prompt.PromptParser;
import com.github.kiu345.eclipse.eclipseai.services.ClientConfiguration;
import com.github.kiu345.eclipse.eclipseai.services.OllamaHttpClient;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class ChatViewPart {
    private Browser browser;

    @Inject
    private OllamaHttpClient httpClient;

    @Inject
    private UISynchronize uiSync;

    @Inject
    private ILog logger;

    @Inject
    private ChatPresenter presenter;

    @Inject
    private DropManager dropManager;

    @Inject
    private ClientConfiguration configuration;

    private Combo modelCombo;

    @SuppressWarnings("unused")
    private Button withVision;
    private Button withFunctionCalls;
    private Scale withTemperature;

    @SuppressWarnings("unused")
    private ScrolledComposite scrolledComposite;

    private List<ModelDescriptor> modelList;

    private List<FileContentAttachment> fileAttachments = new LinkedList<Attachment.FileContentAttachment>();

    @SuppressWarnings("unused")
    private static final String CONTEXT_ADDON_HTML = """
            <div id="context" class="context">
              <div class="header">Context</div>
              <ul id="attachments" class="file-list">
              </ul>
            </div>
            """;

    public ChatViewPart() {
    }

    @Focus
    public void setFocus() {
        browser.setFocus();
    }

    public void clearChatView() {
        uiSync.asyncExec(() -> initializeChatView(browser));
    }

    @PostConstruct
    public void createControls(Composite parent) {
        Composite aiArea = new Composite(parent, SWT.FILL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        aiArea.setLayout(layout);

        Composite browserContainer = new Composite(aiArea, SWT.FILL);
        browserContainer.setLayout(new FillLayout(SWT.VERTICAL));
        browserContainer.setLayoutData(new GridData(GridData.FILL_BOTH)); // Fill both horizontally and vertically

        browser = createChatView(browserContainer);

        Composite controls = new Composite(aiArea, SWT.NONE);
        controls.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button[] buttons = { createAttachFileButton(controls), createClearChatButton(controls), createStopButton(controls), createRefreshButton(controls) };

        controls.setLayout(new GridLayout(buttons.length, false));
        for (var button : buttons) {
            button.setLayoutData(new GridData(SWT.FILL, SWT.RIGHT, true, false));
        }

        modelCombo = new Combo(controls, SWT.DROP_DOWN | SWT.READ_ONLY);
        withTemperature = addScaleField(controls, "Temperature");
        withFunctionCalls = addCheckField(controls, "with Function Calls");

        modelCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                logger.info("Setting model to " + modelCombo.getText());
                configuration.setSelectedModel(modelCombo.getText());
                ModelDescriptor model = httpClient.getModel(modelCombo.getText());
                withFunctionCalls.setEnabled(model.functionCalling());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                logger.info("Setting model to " + modelCombo.getText());
                configuration.setSelectedModel(modelCombo.getText());
                ModelDescriptor model = httpClient.getModel(modelCombo.getText());
                withFunctionCalls.setEnabled(model.functionCalling());
            }
        });

        makeComboList();

        modelCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, buttons.length - 1, 1));

        withTemperature.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                configuration.setTemperature(withTemperature.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                configuration.setTemperature(withTemperature.getSelection());
            }
        });
        withTemperature.setSelection(configuration.getTemperature().orElse(5));

        withFunctionCalls.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                logger.info("Set function usage to " + withFunctionCalls.getSelection());
                configuration.setUseFunctions(withFunctionCalls.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                logger.info("Set function usage to " + withFunctionCalls.getSelection());
                configuration.setUseFunctions(withFunctionCalls.getSelection());
            }
        });

        try {
            ModelDescriptor model = httpClient.getModel(modelCombo.getText());
            boolean supportFunctions = (model == null ? false : model.functionCalling());
            withFunctionCalls.setEnabled(supportFunctions);
            withFunctionCalls.setSelection(configuration.getUseFunctions().orElse(false) && supportFunctions);
        }
        catch (Exception e) {
            logger.warn("failed to set function value: " + e.getMessage());
        }

        dropManager.registerDropTarget(controls);

        clearAttachments();
    }

    private Scale addScaleField(Composite form, String labelText) {
        Scale scale = new Scale(form, SWT.NONE);
        scale.setMinimum(0);
        scale.setMaximum(10);
        scale.setIncrement(1);
        scale.setPageIncrement(1);
        return scale;
    }

    private Button addCheckField(Composite form, String labelText) {
        Button button = new Button(form, SWT.CHECK);
        addFormControl(button, form, labelText);
        return button;
    }

    private Control addFormControl(Control control, Composite form, String labelText) {
        Label label = new Label(form, SWT.NONE);
        label.setText(labelText);
        return control;
    }

    public void makeComboList() {
        modelList = getModelList();

        modelCombo.removeAll();

        for (ModelDescriptor comboItem : modelList) {
            modelCombo.add(comboItem.title(), modelList.indexOf(comboItem));
        }

        if (modelCombo.getItemCount() > 0) {
            modelCombo.select(
                    modelList.indexOf(
                            modelList.stream().filter(t -> t.title().equals(configuration.getSelectedModel().orElse("")))
                                    .findFirst().orElse(null)
                    )
            );
        }
    }

    private List<ModelDescriptor> getModelList() {
        List<ModelDescriptor> modelList = httpClient.getModelList();

        configuration.setModelList(modelList);

        return modelList;
    }

    private Button createAttachFileButton(Composite parent) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText("Attach");
        try {
            Image clearIcon = PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_ADD);
            button.setImage(clearIcon);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    if (window != null) {
                        IEditorPart editorPart = window.getActivePage().getActiveEditor();
                        if (editorPart instanceof ITextEditor) {
                            ITextEditor textEditor = (ITextEditor) editorPart;
                            FileEditorInput fileInput = (FileEditorInput) textEditor.getEditorInput();
                            IFile file = fileInput.getFile();
                            IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());

                            FileContentAttachment fca = new FileContentAttachment(file.getProjectRelativePath().toPortableString(), 0, document.getNumberOfLines(), document.get());

                            if (!fileAttachments.contains(fca)) {
                                fileAttachments.add(fca);
                                logger.info(file.getName());
                                browser.execute("addAttachment('" + file.getProjectRelativePath().toPortableString() + "');");
                            }
                        }
                    }

                }
                catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        return button;
    }

    private Button createClearChatButton(Composite parent) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText("Clear");
        try {
            Image clearIcon = PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_ELCL_REMOVE);
            button.setImage(clearIcon);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fileAttachments.clear();
                presenter.onClear();
            }
        });
        return button;
    }

    private Button createStopButton(Composite parent) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText("Stop");

        Image stopIcon = PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_ELCL_STOP);
        button.setImage(stopIcon);

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                presenter.onStop();
            }
        });
        return button;
    }

    private Button createRefreshButton(Composite parent) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText("Refresh");

        Image stopIcon = PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_ELCL_SYNCED);
        button.setImage(stopIcon);

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshArea();
            }
        });
        return button;
    }

    private void refreshArea() {
        logger.info("Refreshing stuff");

        try {
            makeComboList();
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private Browser createChatView(Composite parent) {
        Browser browser = new Browser(parent, SWT.EDGE);
        initializeChatView(browser);
        initializeFunctions(browser);
        return browser;
    }

    private void initializeFunctions(Browser browser) {
        new CopyCodeFunction(browser, "eclipseCopyCode");
        new SaveCodeFunction(browser, "eclipseSaveCode");
        new ApplyPatchFunction(browser, "eclipseApplyPatch");
        new SendPromptFunction(browser, "eclipseSendPrompt");
    }

    /**
     * Loads the CSS files for the HTML component.
     *
     * @return A concatenated string containing the content of the loaded CSS files.
     */
    private String loadCss() {
        StringBuilder css = new StringBuilder();
//        String[] cssFiles = { "textview.css", "dark.min.css" };
        String[] cssFiles = { "textview.css", "hjthemes.css" };
        for (String file : cssFiles) {
            try (InputStream in = FileLocator
                    .toFileURL(URI.create("platform:/plugin/com.github.kiu345.eclipse.plugin.eclipseai.main/css/" + file).toURL())
                    .openStream()) {
                css.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                css.append("\n");
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return css.toString();
    }

    /**
     * Loads the JavaScript files for the HTML component.
     *
     * @return A concatenated string containing the content of the loaded JavaScript
     *         files.
     */
    private String loadJavaScripts() {
        String[] jsFiles = { "functions.js", "highlight.min.js", "init.js" };
        StringBuilder js = new StringBuilder();
        for (String file : jsFiles) {
            try (InputStream in = FileLocator
                    .toFileURL(URI.create("platform:/plugin/com.github.kiu345.eclipse.plugin.eclipseai.main/js/" + file).toURL())
                    .openStream()) {
                js.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                js.append("\n");
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return js.toString();
    }

    private void initializeChatView(Browser browser) {
        String htmlTemplate = """
                <html>
                  <style>${css}</style>
                  <script>${js}</script>
                  <body>
                    <div class="theme-vs-min">
                      <div id="content">
                        <div id="suggestions" class="chat-bubble"></div>
                        <div id="inputarea" class="chat-bubble me current" contenteditable="plaintext-only" autofocus placeholder="Ask anything, '/' for slash commands"></div>
                        <div id="context" class="context">
                          <div class="header">Context</div>
                          <ul id="attachments" class="file-list">
                          </ul>
                        </div>
                      </div>
                    </div>
                  </body>
                </html>
                """;

        String js = loadJavaScripts();
        String css = loadCss();
        htmlTemplate = htmlTemplate.replace("${js}", js);
        htmlTemplate = htmlTemplate.replace("${css}", css);

        // Initialize the browser with base HTML and CSS
        browser.setText(htmlTemplate);
    }

    public void setMessageHtml(UUID messageId, String messageBody, ChatMessage.Type type) {
        uiSync.asyncExec(() -> {
            PromptParser parser = new PromptParser(messageBody);
            String fixedHtml = escapeHtmlQuotes(fixLineBreaks(parser.parseToHtml(messageId)));
            switch (type) {
                case ERROR:
                    fixedHtml = "<div style=\"background-color: #FFCCCC;\"><p><b>ERROR:</b></p>"+fixedHtml+"</div>";
                    break;
                default:
                    ;
            }
            // inject and highlight html message
            browser.execute(
                    """
                        var element = document.getElementById("%s");
                        element.innerHTML = '%s';
                        hljs.highlightAll();
                    """.formatted("message-" + messageId.toString(), fixedHtml)
            );
            // Scroll down
            browser.execute("window.scrollTo(0, document.body.scrollHeight);");
        });
    }

    public void setInputHtml(UUID messageId, String messageBody) {
        uiSync.asyncExec(() -> {
            InputParser parser = new InputParser(messageBody);

            String fixedHtml = escapeHtmlQuotes(fixLineBreaks(parser.removeLastBr(parser.parseToHtml())));
            // inject and highlight html message
            browser.execute(
                    "var element = document.getElementById(\"message-" + messageId.toString() + "\");" + "element.innerHTML = '"
                            + fixedHtml + "';" + "hljs.highlightElement(element.querySelector('pre code'));"
            );
            // Scroll down
            browser.execute("window.scrollTo(0, document.body.scrollHeight);");
        });
    }

    public void addInputBlock(UUID messageId) {
        uiSync.asyncExec(() -> {
            // inject and highlight html message
            browser.execute(
                    "document.getElementById(\"content\").innerHTML += '"
                            + "<div class=\"chat-bubble me current\" contenteditable=\"plaintext-only\" autofocus placeholder=\"Ask a follow-up\"></div>"
                            + "<div id=\"context\" class=\"context\"><div class=\"header\">Context</div><ul id=\"attachments\" class=\"file-list\"></ul></div>"
                            + "';"
            );
            // Scroll down
            browser.execute("addKeyCapture();");
            browser.execute("window.scrollTo(0, document.body.scrollHeight);");
        });
    }

    /**
     * Replaces newline characters with line break escape sequences in the given
     * string.
     *
     * @param html The input string containing newline characters.
     * @return A string with newline characters replaced by line break escape
     *         sequences.
     */
    private String fixLineBreaks(String html) {
        return html.replace("\n", "\\n").replace("\r", "");
    }

    /**
     * Escapes HTML quotation marks in the given string.
     * 
     * @param html The input string containing HTML.
     * @return A string with escaped quotation marks for proper HTML handling.
     */
    private String escapeHtmlQuotes(String html) {
        return html.replace("\"", "\\\"").replace("'", "\\'");
    }

    public void appendMessage(UUID uuid, String role) {
        String cssClass = "user".equals(role) ? "chat-bubble me" : "chat-bubble you";
        uiSync.asyncExec(() -> {
            browser.execute("""
                    node = document.createElement("div");
                    node.setAttribute("id", "message-${id}");
                    node.setAttribute("class", "${cssClass}");
                    document.getElementById("content").appendChild(node);
                        """.replace("${id}", uuid.toString()).replace("${cssClass}", cssClass));
            browser.execute(
                    // Scroll down
                    "window.scrollTo(0, document.body.scrollHeight);"
            );
        });
    }

    public void insertInputMessageBlock(UUID uuid, String role) {
        //
        String cssClass = "chat-bubble inline";
        uiSync.asyncExec(() -> {
            browser.execute("""
                    parent = document.querySelector(".current");
                    parent.classList.add('inline');
                    node = document.createElement("div");
                    node.setAttribute("id", "message-${id}");
                    node.setAttribute("class", "${cssClass}");
                    parent.parentNode.insertBefore(node, parent);
                    """.replace("${id}", uuid.toString()).replace("${cssClass}", cssClass));
            browser.execute(
                    // Scroll down
                    "window.scrollTo(0, document.body.scrollHeight);"
            );
        });
    }

    public void setInputMessage(String command) {
        uiSync.asyncExec(() -> {
            browser.execute("""
                    setPredefinedPrompt('/${command}');
                    """.replace("${command}", command));
//            browser.execute(
//                    // Scroll down
//                    "window.scrollTo(0, document.body.scrollHeight);" );
        });
    }

    public Object removeMessage(int id) {
        // TODO Auto-generated method stub
        return null;
    }

    public void clearAttachments() {
        setAttachments(Collections.emptyList());
    }

    public void setAttachments(List<Attachment> attachments) {
        /*
         * uiSync.asyncExec(() -> {
         * // Dispose of existing children to avoid memory leaks and remove old
         * // images
         * for (var child : imagesContainer.getChildren()) {
         * child.dispose();
         * }
         * 
         * imagesContainer.setLayout(new RowLayout(SWT.HORIZONTAL));
         * 
         * if (attachments.isEmpty()) {
         * scrolledComposite.setVisible(false);
         * ((GridData) scrolledComposite.getLayoutData()).heightHint = 0;
         * }
         * else {
         * AttachmentVisitor attachmentVisitor = new AttachmentVisitor();
         * 
         * // There are images to display, add them to the imagesContainer
         * for (var attachment : attachments) {
         * attachment.accept(attachmentVisitor);
         * }
         * scrolledComposite.setVisible(true);
         * imagesContainer.setSize(imagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
         * ((GridData) scrolledComposite.getLayoutData()).heightHint = SWT.DEFAULT;
         * }
         * // Refresh the layout
         * updateLayout(imagesContainer);
         * });
         */
    }

    public void updateLayout(Composite composite) {
        if (composite != null) {
            composite.layout();
            updateLayout(composite.getParent());
        }
    }

    public void setInputEnabled(boolean b) {
//        uiSync.asyncExec(() -> {
//            inputArea.setEnabled(b);
//        });
    }

    /**
     * This function establishes a JavaScript-to-Java callback for the browser,
     * allowing the IDE to copy code. It is invoked from JavaScript when the user
     * interacts with the chat view to copy a code block.
     */
    private class CopyCodeFunction extends BrowserFunction {
        public CopyCodeFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments.length > 0 && arguments[0] instanceof String) {
                String codeBlock = (String) arguments[0];
                presenter.onCopyCode(codeBlock);
            }
            return null;
        }
    }

    /**
     * This function establishes a JavaScript-to-Java callback for the browser,
     * allowing the IDE to copy code. It is invoked from JavaScript when the user
     * interacts with the chat view to copy a code block.
     */
    private class SaveCodeFunction extends BrowserFunction {
        public SaveCodeFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments == null || arguments.length != 1) {
                logger.error("Invalid arguments for saveCode");
                return null;
            }
            String codeBlock = (String) arguments[0];

            // Open a file dialog to select the save location
            FileDialog fileDialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
            fileDialog.setFilterPath(System.getProperty("user.home")); // Set default path to user's home directory
            String fileName = fileDialog.open();

            if (fileName != null) {
                try (FileWriter writer = new FileWriter(fileName)) {
                    writer.write(codeBlock);
                }
                catch (IOException e) {
                    logger.error("Error writing to file: " + e.getMessage());
                }
            }

            return null;
        }
    }

    private class SendPromptFunction extends BrowserFunction {
        public SendPromptFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            String userPrompt;
            Boolean isPreDefinedPormpt = false;

            if (arguments.length > 0 && arguments[0] instanceof String) {
                userPrompt = (String) arguments[0];
                if (fileAttachments.size() != 0) {
                    for (FileContentAttachment fca : fileAttachments) {
                        userPrompt = userPrompt + "\n\n" + fca.toChatMessageContent();
                    }
                }

                if (arguments.length > 1 && arguments[1] instanceof Boolean)
                    isPreDefinedPormpt = Boolean.valueOf(arguments[1].toString());

                if (isPreDefinedPormpt) {
                    presenter.onSendPredefinedMessage(userPrompt);
                }
                else {
                    presenter.onSendUserMessage(userPrompt);
                }
                fileAttachments.clear();
            }

            return null;
        }
    }

    /**
     * This function establishes a JavaScript-to-Java callback for the browser,
     * allowing the IDE to copy code. It is invoked from JavaScript when the user
     * interacts with the chat view to copy a code block.
     */
    private class ApplyPatchFunction extends BrowserFunction {
        public ApplyPatchFunction(Browser browser, String name) {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments) {
            if (arguments.length > 0 && arguments[0] instanceof String) {
                String codeBlock = (String) arguments[0];
                presenter.onApplyPatch(codeBlock);
            }
            return null;
        }
    }

}
