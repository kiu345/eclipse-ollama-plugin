package com.github.kiu345.eclipse.eclipseai.external;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

class HtmlMarkdownConverterTest {

    @Test
    void testHtmlToMarkdown() {
        String html = "<h3>Hello World</h3>";

        var options = FlexmarkHtmlConverter.builder().toImmutable();
        var converter = FlexmarkHtmlConverter.builder(options).build();
        String markdown = converter.convert(html);

        assertThat(markdown.trim()).contains("### Hello World");
    }

    @Test
    void testMarkdownToHtml() {
        String markdown = "### Hello World";

        MutableDataSet options = new MutableDataSet();
        //options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Node document = parser.parse(markdown);
        String html = renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"

        assertThat(html.trim()).contains("<h3>Hello World</h3>");
    }
}
