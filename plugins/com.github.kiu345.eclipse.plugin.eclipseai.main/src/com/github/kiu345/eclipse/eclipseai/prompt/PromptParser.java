package com.github.kiu345.eclipse.eclipseai.prompt;

import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * A utility class for parsing and converting a text prompt to an HTML formatted string.
 */
public class PromptParser {

    private static final int DEFAULT_STATE = 0;
    private static final int CODE_BLOCK_STATE = 1;
    private static final int FUNCION_CALL_STATE = 2;
    private static final int TEXT_ATTACHMENT_STATE = 4;

    private static final String TATT_CONTEXTSTART = "<|ContextStart|>";
    private static final String TATT_FILEPREFIX = "File: ";
    private static final String TATT_LINESPREFIX = "Lines: ";
    private static final String TATT_CONTENTSTART = "<|ContentStart|>";
    private static final String TATT_CONTENTEND = "<|ContentEnd|>";
    private static final String TATT_CONTEXTEND = "<|ContextEnd|>";

    private int state = DEFAULT_STATE;

    private static final String START_THINK = "<think>";
    private static final String END_THINK = "</think>";

    private String prompt;
    private Pattern codeBlockPattern = Pattern.compile("^(\\s*)```([aA-zZ]*)$");
    private Pattern functionCallPattern = Pattern.compile("^\"function_call\".*");

    public PromptParser(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Converts the prompt text to an HTML formatted string.
     *
     * @return An HTML formatted string representation of the prompt text.
     */
    public String parseToHtml() {
        var out = new StringBuilder();

        var thinkString = "";
        
        if (prompt.startsWith("<think>")) {
            int think_end = prompt.indexOf(END_THINK);
            out.append("<div class=\"thinking\">");
            if (think_end >= 0) {
                thinkString = prompt.substring(START_THINK.length(),think_end);
                prompt = prompt.substring(think_end+END_THINK.length());
                out.append(StringEscapeUtils.escapeHtml4(thinkString));
                out.append("</div>");
            }
            else {
                thinkString = prompt.substring(START_THINK.length());
                out.append(StringEscapeUtils.escapeHtml4(thinkString));
                out.append("</div>");
                return out.toString();
            }
        }

        try (var scanner = new Scanner(prompt)) {
            scanner.useDelimiter("\n");
            StringBuilder textBuffer = new StringBuilder();
            while (scanner.hasNext()) {
                var line = scanner.next();
                var codeBlockMatcher = codeBlockPattern.matcher(line);
                var functionBlockMatcher = functionCallPattern.matcher(line);

                if (codeBlockMatcher.find()) {
                    if (!textBuffer.isEmpty()) {
                        handleNonCodeBlock(out, textBuffer.toString(), !scanner.hasNext());
                        textBuffer = new StringBuilder();
                    }
                    var indentSize = codeBlockMatcher.group(1).length();
                    var lang = codeBlockMatcher.group(2);
                    handleCodeBlock(out, lang, indentSize);
                }
                else if (functionBlockMatcher.find()) {
                    if (!textBuffer.isEmpty()) {
                        handleNonCodeBlock(out, textBuffer.toString(), !scanner.hasNext());
                        textBuffer = new StringBuilder();
                    }
                    handleFunctionCall(out, line);
                }
                else if (line.startsWith(TATT_CONTEXTSTART)) {
                    if (!textBuffer.isEmpty()) {
                        handleNonCodeBlock(out, textBuffer.toString(), !scanner.hasNext());
                        textBuffer = new StringBuilder();
                    }
                    handleTextAttachmentStart(out, line);
                }
                else {
                   textBuffer.append(line+"\n");
                }
            }
            if (!textBuffer.isEmpty()) {
                handleNonCodeBlock(out, textBuffer.toString(), false);
            }
        }
        return out.toString();
    }

    private void handleTextAttachmentStart(StringBuilder out, String line) {
        if ((state & TEXT_ATTACHMENT_STATE) != TEXT_ATTACHMENT_STATE) {
            out.append("""

                    <div class="function-call">
                    <details><summary>""");
            state ^= TEXT_ATTACHMENT_STATE;
        }

    }

    private void handleFunctionCall(StringBuilder out, String line) {
        if ((state & FUNCION_CALL_STATE) != FUNCION_CALL_STATE) {
            out.append(
                    """
                            <div class="function-call">
                            <details><summary>Function call</summary>
                            <pre>
                    """ + line

            );
            state ^= FUNCION_CALL_STATE;

        }
    }

    private void handleNonCodeBlock(StringBuilder out, String line, boolean lastLine) {
        if ((state & CODE_BLOCK_STATE) == CODE_BLOCK_STATE) {
            out.append(StringEscapeUtils.escapeHtml4(escapeBackSlashes(line)));
        }
        else if ((state & TEXT_ATTACHMENT_STATE) == TEXT_ATTACHMENT_STATE) {
            handleTextAttachmentLine(out, line);
            return;
        }
        else {
            out.append(markdown(line));
        }

        if (lastLine && (state & CODE_BLOCK_STATE) == CODE_BLOCK_STATE) // close opened code blocks
        {
            out.append("</code></pre>\n");
        }
        if (lastLine && (state & FUNCION_CALL_STATE) == FUNCION_CALL_STATE) // close opened code blocks
        {
            out.append("</pre></div>\n");
        }
        else if ((state & CODE_BLOCK_STATE) == CODE_BLOCK_STATE) {
            out.append("\n");
        }
        else if (!lastLine) {
            out.append("<br/>");
        }
    }

    private void handleTextAttachmentLine(StringBuilder out, String line) {
        if (line.startsWith(TATT_FILEPREFIX)) {
            out.append("Context: " + line.substring(TATT_FILEPREFIX.length()) + ", ");
        }
        else if (line.startsWith(TATT_LINESPREFIX)) {
            out.append(line + "</summary>");
        }
        else if (line.startsWith(TATT_CONTENTSTART)) {
            out.append("<pre>\n");
        }
        else if (line.startsWith(TATT_CONTENTEND)) {
            out.append("\n</pre>");
        }
        else if (line.startsWith(TATT_CONTEXTEND)) {
            out.append("\n</details></div>\n");
            state ^= TEXT_ATTACHMENT_STATE;
        }
        else {
            out.append(StringEscapeUtils.escapeHtml4(line) + "<br/>");
        }
    }

    private void handleCodeBlock(StringBuilder out, String lang, int indent) {
        if ((state & CODE_BLOCK_STATE) != CODE_BLOCK_STATE) {
            String codeBlockId = UUID.randomUUID().toString();
            out.append(
                    """
                            <input type="button" onClick="eclipseCopyCode(document.getElementById('${codeBlockId}').innerText)" value="Copy" />
                            <input type="button" onClick="eclipseSaveCode(document.getElementById('${codeBlockId}').innerText)" value="Save" />
                            <input type="${showApplyPatch}" onClick="eclipseApplyPatch(document.getElementById('${codeBlockId}').innerText)" value="ApplyPatch"/>
                            <pre style="margin-left: ${indent}pt;"><code lang="${lang}" id="${codeBlockId}">
                            """
                            .replace("${indent}", "" + (indent * 5))
                            .replace("${lang}", lang)
                            .replace("${codeBlockId}", codeBlockId)
                            .replace("${showApplyPatch}", "diff".equals(lang) ? "button" : "hidden") // show "Apply Patch" button for diffs
            );
            state ^= CODE_BLOCK_STATE;
        }
        else {
            out.append("</code></pre>\n");
            state ^= CODE_BLOCK_STATE;
        }
    }

    public static String escapeBackSlashes(String input) {
        input = input.replace("\\", "\\\\");
        return input;
    }

    public static String markdown(String input) {
        /*
        // Replace headers
        input = input.replaceAll("^# (.*?)$", "<h1>$1</h1>");
        input = input.replaceAll("^## (.*?)$", "<h2>$1</h2>");
        input = input.replaceAll("^### (.*?)$", "<h3>$1</h3>");
        input = input.replaceAll("^#### (.*?)$", "<h4>$1</h4>");
        input = input.replaceAll("^##### (.*?)$", "<h5>$1</h5>");
        input = input.replaceAll("^###### (.*?)$", "<h6>$1</h6>");

        // Replace **text** with <strong>text</strong>
        input = input.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");

        // Replace *text* with <em>text</em>
        input = input.replaceAll("\\*(.*?)\\*", "<em>$1</em>");

        // Replace `text` with <i>text</i>
        input = input.replaceAll("`(.*?)`", "<i>$1</i>");

        // Replace ![alt text](url) with <img src="url" alt="alt text">
        input = input.replaceAll("!\\[(.*?)\\]\\((.*?)\\)", "<img src=\"$2\" alt=\"$1\" />");

        // Replace [text](url) with <a href="url">text</a>
        input = input.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "<a href=\"$2\" target=\"_blank\">$1</a>");

        // Inline code
        input = input.replaceAll("`([^`]+)`", "<code>$1</code>");

        // Links
        input = input.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "<a href=\"$2\" target=\"_blank\">$1</a>");

        // Blockquotes
        input = input.replaceAll("^> (.*?)$", "<blockquote>$1</blockquote>");

        // Unordered lists
        input = input.replaceAll("^\\* (.*?)$", "<li>$1</li>");
        input = input.replaceAll("^- (.*?)$", "<li>$1</li>");
        input = input.replaceAll("^\\+ (.*?)$", "<li>$1</li>");

        // Ordered lists
//        input = input.replaceAll("^\\d+\\. (.*?)$", "<li>$1</li>");

        // Horizontal Rule
        input = input.replaceAll("^(\\*\\*\\*|---)$", "<hr>");
*/

        MutableDataSet options = new MutableDataSet();
        // uncomment to set optional extensions
        //options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Node document = parser.parse(input);
        String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"

        return html;
    }
}
