package com.github.kiu345.eclipse.assistai.part;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.PlatformUI;

import com.github.kiu345.eclipse.assistai.model.ModelDescriptor;
import com.github.kiu345.eclipse.assistai.part.Attachment.UiVisitor;
import com.github.kiu345.eclipse.assistai.part.dnd.DropManager;
import com.github.kiu345.eclipse.assistai.prompt.InputParser;
import com.github.kiu345.eclipse.assistai.prompt.PromptParser;
import com.github.kiu345.eclipse.assistai.services.ClientConfiguration;
import com.github.kiu345.eclipse.assistai.services.OllamaHttpClient;

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

    private LocalResourceManager resourceManager;

//    private Text inputArea;

    private Combo modelCombo;

    @SuppressWarnings("unused")
    private Button withVision;
    @SuppressWarnings("unused")
    private Button withFunctionCalls;
    private Scale withTemperature;

    private ScrolledComposite scrolledComposite;

    private Composite imagesContainer;

    private List<ModelDescriptor> modelList;

    public ChatViewPart() {
    }

    @Focus
    public void setFocus() {
        browser.setFocus();
    }

    public void clearChatView() {
        uiSync.asyncExec(() -> initializeChatView(browser));
    }

//    public void clearUserInput() {
//        uiSync.asyncExec(() -> {
//            inputArea.setText("");
//        });
//    }

    @PostConstruct
    public void createControls(Composite parent) {
        resourceManager = new LocalResourceManager(JFaceResources.getResources());

        SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite browserContainer = new Composite(sashForm, SWT.VERTICAL);
        browserContainer.setLayout(new FillLayout(SWT.VERTICAL));

        browser = createChatView(browserContainer);

        // Create the JavaScript-to-Java callback
//        new CopyCodeFunction(browser, "eclipseFunc");
//        new SaveCodeFunction(browser, "eclipseFunc");

        Composite controls = new Composite(sashForm, SWT.NONE);

        Composite attachmentsPanel = createAttachmentsPanel(controls);
//        inputArea = createUserInput(controls);
        // create components
        Button[] buttons = { createClearChatButton(controls), createStopButton(controls), createRefreshButton(controls) };

        // layout components
        controls.setLayout(new GridLayout(buttons.length, false));
        attachmentsPanel.setLayoutData(new GridData(SWT.FILL, SWT.PUSH, true, false, buttons.length, 1)); // Full width
//        inputArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, buttons.length, 1)); // colspan = num of
        // buttons
        for (var button : buttons) {
            button.setLayoutData(new GridData(SWT.FILL, SWT.RIGHT, true, false));
        }

        modelCombo = new Combo(controls, SWT.DROP_DOWN | SWT.READ_ONLY);

        modelCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                logger.info("Setting model to " + modelCombo.getText());
                configuration.setSelectedModel(modelCombo.getText());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                logger.info("Setting model to " + modelCombo.getText());
                configuration.setSelectedModel(modelCombo.getText());
            }
        });

        makeComboList();

        modelCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, buttons.length, 1));

//        withVision = addCheckField( controls, "With Vision:");
//        withFunctionCalls = addCheckField( controls, "With Function Calls:");
        withTemperature = addScaleField(controls, "Temperature");
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

        // Sets the initial weight ratio: 75% browser, 25% controls
        sashForm.setWeights(new int[] { 80, 20 });

        // Enable DnD for the controls below the chat view
        dropManager.registerDropTarget(controls);

        clearAttachments();
    }

    private Scale addScaleField(Composite form, String labelText) {
        Scale scale = new Scale(form, SWT.NONE);
        scale.setMinimum(0);
        scale.setMaximum(10);
        scale.setIncrement(1);
        scale.setPageIncrement(1);
//        addFormControl( scale, form, labelText);
        return scale;
    }

    @SuppressWarnings("unused")
    private Button addCheckField(Composite form, String labelText) {
        Button button = new Button(form, SWT.CHECK);
        addFormControl(button, form, labelText);
        return button;
    }

    private Control addFormControl(Control control, Composite form, String labelText) {
        Label label = new Label(form, SWT.NONE);
        label.setText(labelText);
        FormData labelData = new FormData();
        Control[] children = form.getChildren();
        if (children.length == 2) {
            // First control, so attach it to the top of the form
            labelData.top = new FormAttachment(0, 10);
        }
        else {
            // Attach it below the last control
            Control lastControl = children[children.length - 3];
            labelData.top = new FormAttachment(lastControl, 10);
        }
        labelData.left = new FormAttachment(0, 10);
        label.setLayoutData(labelData);

        FormData textData = new FormData();
        textData.left = new FormAttachment(0, 150);
        textData.right = new FormAttachment(100, -10);
        textData.top = new FormAttachment(label, -2, SWT.TOP);
        control.setLayoutData(textData);
        return control;
    }

//    private IPropertyChangeListener apiKeyListener = e -> {
//        if( PreferenceConstants.AION_BASE_URL.equals( e.getProperty() ) ||
//        		PreferenceConstants.AION_GET_MODEL_API_PATH.equals( e.getProperty() ) ||
//        		PreferenceConstants.AION_API_BASE_URL.equals( e.getProperty() ) ||
//        		PreferenceConstants.AION_API_KEY.equals( e.getProperty() ) 
//        		)
//        {
//            makeComboList(null);        	
//        }
//    };

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

    private Composite createAttachmentsPanel(Composite parent) {
        Composite attachmentsPanel = new Composite(parent, SWT.NONE);
        attachmentsPanel.setLayout(new GridLayout(1, false)); // One column

        scrolledComposite = new ScrolledComposite(attachmentsPanel, SWT.H_SCROLL);
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        imagesContainer = new Composite(scrolledComposite, SWT.NONE);
        imagesContainer.setLayout(new RowLayout(SWT.HORIZONTAL));

        scrolledComposite.setContent(imagesContainer);
        scrolledComposite.setMinSize(imagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        return attachmentsPanel;
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
                presenter.onClear();
            }
        });
        return button;
    }

    private Button createStopButton(Composite parent) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText("Stop");

        // Use the built-in 'IMG_ELCL_STOP' icon
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

//    private Text createUserInput(Composite parent) {
//        Text inputArea = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
//        inputArea.addTraverseListener(new TraverseListener() {
//            public void keyTraversed(TraverseEvent e) {
//                if (e.detail == SWT.TRAVERSE_RETURN && (e.stateMask & SWT.MODIFIER_MASK) == 0) {
//                    presenter.onSendUserMessage(inputArea.getText());
//                }
//            }
//        });
//        createCustomMenu(inputArea);
//        return inputArea;
//    }

    private void refreshArea() {
        logger.info("Refreshing stuff");

        try {
            makeComboList();
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    /**
     * Dynamically creates and assigns a custom context menu to the input area.
     * <p>
     * This method constructs a context menu with "Cut", "Copy", and "Paste" actions
     * for the text input area. The "Paste" action is conditionally enabled based on
     * the current content of the clipboard: it's enabled if the clipboard contains
     * either text or image data. When triggered, the "Paste" action checks the
     * clipboard content type and handles it accordingly - pasting text directly
     * into the input area or invoking a custom handler for image data.
     *
     * @param inputArea The Text widget to which the custom context menu will be
     *                  attached.
     */
//    private void createCustomMenu(Text inputArea) {
//        Menu menu = new Menu(inputArea);
//        inputArea.setMenu(menu);
//        menu.addMenuListener(new MenuAdapter() {
//            @Override
//            public void menuShown(MenuEvent e) {
//                // Dynamically adjust the context menu
//                MenuItem[] items = menu.getItems();
//                for (MenuItem item : items) {
//                    item.dispose();
//                }
//                // Add Cut, Copy, Paste items
//                addMenuItem(menu, "Cut", () -> inputArea.cut());
//                addMenuItem(menu, "Copy", () -> inputArea.copy());
//                MenuItem pasteItem = addMenuItem(menu, "Paste", () -> handlePasteOperation());
//                // Enable or disable paste based on clipboard content
//                Clipboard clipboard = new Clipboard(Display.getCurrent());
//                boolean enablePaste = clipboard.getContents(TextTransfer.getInstance()) != null
//                        || clipboard.getContents(ImageTransfer.getInstance()) != null;
//                pasteItem.setEnabled(enablePaste);
//                clipboard.dispose();
//            }
//        });
//    }
//
//    private MenuItem addMenuItem(Menu parent, String text, Runnable action) {
//        MenuItem item = new MenuItem(parent, SWT.NONE);
//        item.setText(text);
//        item.addListener(SWT.Selection, e -> action.run());
//        return item;
//    }
//
//    private void handlePasteOperation() {
//        Clipboard clipboard = new Clipboard(Display.getCurrent());
//
//        if (clipboard.getContents(ImageTransfer.getInstance()) != null) {
//            ImageData imageData = (ImageData) clipboard.getContents(ImageTransfer.getInstance());
//            presenter.onAttachmentAdded(imageData);
//        }
//        else {
//            String textData = (String) clipboard.getContents(TextTransfer.getInstance());
//            if (textData != null) {
//                inputArea.insert(textData); // Manually insert text at the
//                                            // current caret position
//            }
//
//        }
//    }

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

    private void initializeChatView(Browser browser) {
        String htmlTemplate = """
                <html>
                  <style>${css}</style>
                  <script>${js}</script>
                  <body>
                    <div class="theme-vs-min">
                      <div id="content">
                        <div class="chat-bubble" id="suggestions"></div>
                        <div class="chat-bubble me current" contenteditable="true" autofocus placeholder="Ask anything, '/' for slash commands"></div>
                        <div class="context" id="context">
                          <div class="header">Context</div>
                          <!-- <ul class="file-list">
                            <li class="file-item">file1.txt<a href="#" class="remove-cross">&times;</a></li>
                          </ul> -->
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

    /**
     * Loads the CSS files for the ChatGPTViewPart component.
     *
     * @return A concatenated string containing the content of the loaded CSS files.
     */
    private String loadCss() {
        StringBuilder css = new StringBuilder();
//        String[] cssFiles = { "textview.css", "dark.min.css" };
        String[] cssFiles = { "textview.css", "hjthemes.css" };
        for (String file : cssFiles) {
            try (InputStream in = FileLocator
                    .toFileURL(URI.create("platform:/plugin/com.github.kiu345.eclipse.plugin.assistai.main/css/" + file).toURL())
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
     * Loads the JavaScript files for the ChatGPTViewPart component.
     *
     * @return A concatenated string containing the content of the loaded JavaScript
     *         files.
     */
    private String loadJavaScripts() {
        String[] jsFiles = { "functions.js", "highlight.min.js" };
        StringBuilder js = new StringBuilder();
        for (String file : jsFiles) {
            try (InputStream in = FileLocator
                    .toFileURL(URI.create("platform:/plugin/com.github.kiu345.eclipse.plugin.assistai.main/js/" + file).toURL())
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

    public void setMessageHtml(String messageId, String messageBody) {
//        logger.info("setting messaage " + messageId + ":" + messageBody);
        uiSync.asyncExec(() -> {
            PromptParser parser = new PromptParser(messageBody);

            String fixedHtml = escapeHtmlQuotes(fixLineBreaks(parser.parseToHtml()));
            // inject and highlight html message
            browser.execute(
                    "var element = document.getElementById(\"message-" + messageId + "\");" + "element.innerHTML = '"
                            + fixedHtml + "';" + "hljs.highlightElement(element.querySelector('pre code'));"
            );
            // Scroll down
            browser.execute("window.scrollTo(0, document.body.scrollHeight);");
        });
    }

    public void setInputHtml(String messageId, String messageBody) {
        uiSync.asyncExec(() -> {
            InputParser parser = new InputParser(messageBody);

            String fixedHtml = escapeHtmlQuotes(fixLineBreaks(parser.removeLastBr(parser.parseToHtml())));
            // inject and highlight html message
            browser.execute(
                    "var element = document.getElementById(\"message-" + messageId + "\");" + "element.innerHTML = '"
                            + fixedHtml + "';" + "hljs.highlightElement(element.querySelector('pre code'));"
            );
            // Scroll down
            browser.execute("window.scrollTo(0, document.body.scrollHeight);");
        });
    }

//    public void addInputBlock(String messageId)
//    {
//        uiSync.asyncExec( () -> {
//            // inject and highlight html message
//            browser.execute( "document.getElementById(\"content\").innerHTML += '" + 
//            "<div class=\"chat-bubble me\" contenteditable=\"true\" id=\"message-" + messageId + "\" "
//            		+ "placeholder=\"Ask a follow-up\"></div>" + "';"
//            		+ "addKeyCapture(document.getElementById(\"message-" + messageId + "\"));");
//            // Scroll down
//            browser.execute( "window.scrollTo(0, document.body.scrollHeight);" );
//            browser.execute( "document.getElementById(\"message-" + messageId + "\").focus();" );
//        } );
//    }
    public void addInputBlock(String messageId) {
        uiSync.asyncExec(() -> {
            // inject and highlight html message
            browser.execute(
                    "document.getElementById(\"content\").innerHTML += '"
                            + "<div class=\"chat-bubble me current\" contenteditable=\"true\" autofocus placeholder=\"Ask a follow-up\"></div>"
                            + "<div class=\"context\" id=\"context\"><div class=\"header\">Context</div></div>"
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

    public void appendMessage(String messageId, String role) {
        logger.info("Adding message");
        String cssClass = "user".equals(role) ? "chat-bubble me" : "chat-bubble you";
        uiSync.asyncExec(() -> {
            browser.execute("""
                    node = document.createElement("div");
                    node.setAttribute("id", "message-${id}");
                    node.setAttribute("class", "${cssClass}");
                    document.getElementById("content").appendChild(node);
                        """.replace("${id}", messageId).replace("${cssClass}", cssClass));
            browser.execute(
                    // Scroll down
                    "window.scrollTo(0, document.body.scrollHeight);"
            );
        });
    }

    public void insertInputMessageBlock(String messageId, String role) {
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
                    """.replace("${id}", messageId).replace("${cssClass}", cssClass));
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
        uiSync.asyncExec(() -> {
            // Dispose of existing children to avoid memory leaks and remove old
            // images
            for (var child : imagesContainer.getChildren()) {
                child.dispose();
            }

            imagesContainer.setLayout(new RowLayout(SWT.HORIZONTAL));

            if (attachments.isEmpty()) {
                scrolledComposite.setVisible(false);
                ((GridData) scrolledComposite.getLayoutData()).heightHint = 0;
            }
            else {
                AttachmentVisitor attachmentVisitor = new AttachmentVisitor();

                // There are images to display, add them to the imagesContainer
                for (var attachment : attachments) {
                    attachment.accept(attachmentVisitor);
                }
                scrolledComposite.setVisible(true);
                imagesContainer.setSize(imagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                ((GridData) scrolledComposite.getLayoutData()).heightHint = SWT.DEFAULT;
            }
            // Refresh the layout
            updateLayout(imagesContainer);
        });
    }

    private class AttachmentVisitor implements UiVisitor {
        private Label imageLabel;

        @Override
        public void add(ImageData preview, String caption) {
            imageLabel = new Label(imagesContainer, SWT.NONE);
            // initially nothing is selected
            imageLabel.setData("selected", false);
            imageLabel.setToolTipText(caption);

            ImageDescriptor imageDescriptor;
            try {
                imageDescriptor = Optional.ofNullable(preview)
                        .map(id -> ImageDescriptor.createFromImageDataProvider(zoom -> id))
                        .orElse(
                                ImageDescriptor
                                        .createFromURL(URI.create("platform:/plugin/com.github.kiu345.eclipse.plugin.assistai.main/icons/folder.png").toURL())
                        );
            }
            catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }

            Image scaledImage = resourceManager.createImageWithDefault(imageDescriptor);
            Image selectedImage = createSelectedImage(scaledImage);

            imageLabel.setImage(scaledImage);

            imageLabel.addDisposeListener(l -> {
                resourceManager.destroy(imageDescriptor);
                selectedImage.dispose();
            });

            // Add mouse listener to handle selection
            imageLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    boolean isSelected = (boolean) imageLabel.getData("selected");
                    imageLabel.setData("selected", !isSelected);

                    if (isSelected) {
                        imageLabel.setImage(scaledImage);
                    }
                    else {
                        // If it was not selected, apply an overlay
                        Image selectedImage = createSelectedImage(scaledImage);
                        imageLabel.setImage(selectedImage);
                        // Dispose the tinted image when the label is
                        // disposed
                        imageLabel.addDisposeListener(l -> selectedImage.dispose());
                    }
                    imagesContainer.layout();
                }
            });
        }
    }

    private Image createSelectedImage(Image originalImage) {
        // Create a new image that is a copy of the original
        Image tintedImage = new Image(Display.getCurrent(), originalImage.getBounds());

        // Create a GC to draw on the tintedImage
        GC gc = new GC(tintedImage);

        // Draw the original image onto the new image
        gc.drawImage(originalImage, 0, 0);

        // Set alpha value for the overlay (128 is half-transparent)
        gc.setAlpha(128);

        // Get the system selection color
        Color selectionColor = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_SELECTION);

        // Fill the image with the selection color overlay
        gc.setBackground(selectionColor);
        gc.fillRectangle(tintedImage.getBounds());

        // Dispose the GC to free up system resources
        gc.dispose();

        return tintedImage;
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
                System.err.println("Invalid arguments for saveCode");
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
                } catch (IOException e) {
                    System.err.println("Error writing to file: " + e.getMessage());
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

                if (arguments.length > 1 && arguments[1] instanceof Boolean)
                    isPreDefinedPormpt = Boolean.valueOf(arguments[1].toString());

                if (isPreDefinedPormpt) {
                    presenter.onSendPredefinedMessage(userPrompt);
                }
                else {
                    presenter.onSendUserMessage(userPrompt);
                }
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
