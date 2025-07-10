package com.github.kiu345.eclipse.eclipseai.prompt;

import java.util.Arrays;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.html.MutableAttributes;

public abstract class UIParser {
    protected static final String TATT_CONTEXTSTART = "<|ContextStart|>";
    protected static final String TATT_FILEPREFIX = "File: ";
    protected static final String TATT_LINESPREFIX = "Lines: ";
    protected static final String TATT_CONTENTSTART = "<|ContentStart|>";
    protected static final String TATT_CONTENTEND = "<|ContentEnd|>";
    protected static final String TATT_CONTEXTEND = "<|ContextEnd|>";

    public static String escapeBackSlashes(String input) {
        return input.replace("\\", "\\\\");
    }

    public static String markdown(String input) {
        MutableDataSet options = new MutableDataSet();
        // uncomment to set optional extensions
        // options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
        // uncomment to convert soft-breaks to hard breaks
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
        // options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options)
                .attributeProviderFactory(createAttributeProviderFactory())
                .build();

        Node document = parser.parse(input);
        String html = renderer.render(document); // "<p>This is <em>Sparta</em></p>\n"

        return html;
    }

    /*
     * open links in new window
     */
    public static IndependentAttributeProviderFactory createAttributeProviderFactory() {
        return new IndependentAttributeProviderFactory() {
            @Override
            public AttributeProvider apply(LinkResolverContext context) {
                return new AttributeProvider() {
                    @Override
                    public void setAttributes(@NotNull Node node, @NotNull AttributablePart part, @NotNull MutableAttributes attributes) {
                        if (node instanceof Link && part == AttributablePart.LINK) {
                            attributes.replaceValue("target", "_blank");
                            attributes.replaceValue("rel", "noopener noreferrer");
                        }
                        if (node instanceof TableBlock && part == AttributablePart.NODE) {
                            attributes.addValue("class","chat");
                        }
                    }
                };
            }
        };
    }

    /**
     * Converts the text to an HTML formatted string.
     *
     * @return An HTML formatted string representation of the prompt text.
     */
    abstract public String parseToHtml(UUID msgUuid, String prompt);

}
