package com.github.kiu345.eclipse.eclipseai.prompt;

import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

/**
 * A utility class for parsing and converting a text prompt to an HTML formatted string.
 */
public class MessageParser extends UIParser {

    private static final int DEFAULT_STATE = 0;
    private static final int CODE_BLOCK_STATE = 1;
    private static final int FUNCION_CALL_STATE = 2;
    private static final int TEXT_ATTACHMENT_STATE = 4;

    private int state = DEFAULT_STATE;

    private static final String START_THINK = "<think>";
    private static final String END_THINK = "</think>";

    private Pattern codeBlockPattern = Pattern.compile("^(\\s*)```([aA-zZ]*)$");
    private Pattern functionCallPattern = Pattern.compile("^\"function_call\".*");

    @Override
    public String parseToHtml(UUID msgUuid, String prompt) {
        var out = new StringBuilder();

        var thinkString = "";
        
        if (prompt.trim().equalsIgnoreCase("</think>")) {
            return "<div class=\"thinking\">Thinking...</div>";
        }
        
        if (prompt.startsWith(START_THINK)) {
            int think_end = prompt.indexOf(END_THINK);
            out.append("<div class=\"thinking\">");
            out.append("<div class=\"header\">Thought<a class=\"headertools\" onClick=\"toggelView('thought_"+msgUuid.toString()+"')\">Show/hide</a></div>");
            out.append("<div id=\"thought_"+msgUuid.toString()+"\" class=\"thought\" style=\"display: none;\">");
            if (think_end >= 0) {
                thinkString = prompt.substring(START_THINK.length(),think_end);
                prompt = prompt.substring(think_end+END_THINK.length());
                out.append(StringEscapeUtils.escapeHtml4(thinkString));
                out.append("</div></div>");
            }
            else {
                thinkString = prompt.substring(START_THINK.length());
                out.append(StringEscapeUtils.escapeHtml4(thinkString));
                out.append("</div></div>");
                return out.toString();
            }
        }
        prompt = StringEscapeUtils.escapeHtml4(prompt);

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
                   textBuffer.append(line);
//                   if (scanner.hasNext()) {
//                       handleNonCodeBlock(out, textBuffer.toString(), !scanner.hasNext());
                       if (scanner.hasNext()) {
                           textBuffer.append("\n");
                       }
//                   }
                }
            }
            if (!textBuffer.isEmpty()) {
                handleNonCodeBlock(out, textBuffer.toString(), !scanner.hasNext());
            }
        }
        return out.toString().trim();
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
}
