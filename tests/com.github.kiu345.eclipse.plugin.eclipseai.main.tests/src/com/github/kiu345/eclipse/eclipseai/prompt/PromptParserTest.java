package com.github.kiu345.eclipse.eclipseai.prompt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.kiu345.eclipse.eclipseai.part.ChatViewPart;

class PromptParserTest {

    private static final String TEST1 = """
            **The Fox of Ember Hollow**
            <p>In a village where the rivers ran <strong>silver</strong> and the skies hummed with whispers, there lived a boy named Leo who feared the woods.</p>
            <div class="forest">
                <p>One autumn, he stumbled upon a fox with eyes like molten gold, its fur shimmering with the faint glow of a dying star.</p>
                <p><strong>The fox</strong> asked, "Why do you fear the woods, child?"</p>
                <p>Leo muttered about shadows and the tales of the Wraithwood, where lost souls lingered.</p>
            </div>
            <div class="boat">
                <p>Curious, Leo followed the fox deep into the woods. The trees grew taller, their branches weaving a canopy of twilight.</p>
                <p>The fox led him to a clearing where a child's toy boat floated on a pond of starlight.</p>
                <p>The boat was frozen, its sail tattered.</p>
            </div>
            <p><strong>The fox</strong> said, "Once, this boat carried a boy who sought answers. He asked the woods for a wish. The woods gave him a path to escape his troubles. But the boy forgot to return. The boat is a tomb."</p>
            <p>Leo's chest ached. "How do I fix it?"</p>
            <p>The fox licked its lips, the sound like distant thunder. "You must sail the boat back. But the stars will not guide you unless you let go of your fear."</p>
            <p>Leo knelt, brushing the dust from the boat. As he touched it, the pond rippled, and the boat drifted toward the trees.</p>
            <p>When Leo returned home, the woods felt different—less ominous, more alive. He never spoke of the fox, but the feather, tucked in his pocket, glowed whenever he faced a choice.</p>""";
    private static final String TEST2 = "<p>Leo's chest ached. \"How do I fix it?\"</p><br/>2>1";

    private static final String TEST3 = "Leo's chest ached. \"How do I fix it?\"\n2>1";

    private static final String TEST4 = """
            <div class="boat">
                <p>Curious, Leo followed the fox deep into the woods. The trees grew taller, their branches weaving a canopy of twilight.</p>
                <p>The fox led him to a clearing where a child's toy boat floated on a pond of starlight.</p>
                <p>The boat was frozen, its sail tattered.</p>
            </div>""";
    private static final String TEST5 = """
            <think>
              I should do something...
            </think>
            Do it or don't, doesn't matter...""";

    private PromptParser parser;

    @Test
    void testParseToHtml1() {
        System.out.println("PromptParserTest.testParseToHtml1()");
        parser = new PromptParser();
        String result = parser.parseToHtml(UUID.randomUUID(), TEST1);
        assertNotNull(result);
        System.out.println(result);
    }

    @Test
    void testParseToHtml2() {
        System.out.println("PromptParserTest.testParseToHtml2()");
        System.out.println(TEST2 + "\n---");
        parser = new PromptParser();
        String result = parser.parseToHtml(UUID.randomUUID(), TEST2);
        System.out.println(result);
        assertNotNull(result);
        assertTrue(result.trim().matches("<p>[^<>]*</p>"), "< and > should be escaped in content");
    }

    @Test
    void testParseToHtml3() {
        System.out.println("PromptParserTest.testParseToHtml3()");
        System.out.println(TEST3 + "\n---");
        parser = new PromptParser();
        String result = parser.parseToHtml(UUID.randomUUID(), TEST3);
        assertNotNull(result);
        System.out.println(result);
    }

    @Test
    void testParseToHtmlWithOtherFunctions() {
        System.out.println("PromptParserTest.testParseToHtmlWithOtherFunctions()");
        System.out.println(TEST4 + "\n---");
        parser = new PromptParser();
        String result = ChatViewPart.escapeHtmlQuotes(ChatViewPart.fixLineBreaks(parser.parseToHtml(UUID.randomUUID(), TEST4)));
        assertNotNull(result);
        System.out.println(result);
    }

    @Test
    void testParseToHtml5() {
        System.out.println("PromptParserTest.testParseToHtml5()");
        System.out.println(TEST5 + "\n---");
        parser = new PromptParser();
        String result = parser.parseToHtml(UUID.randomUUID(), TEST5);
        assertNotNull(result);
        System.out.println(result);
    }

    @Test
    void testEscapeBackSlashes() {
        System.out.println("PromptParserTest.testEscapeBackSlashes()");
        String result = PromptParser.escapeBackSlashes("\\b");
        assertNotNull(result);
        assertEquals("\\\\b", result);
    }

    @Test
    void testMarkdown() {
        System.out.println("PromptParserTest.testMarkdown()");
        String result = PromptParser.markdown("*bold*");
        assertNotNull(result);
        assertEquals("<p><em>bold</em></p>", result.trim());
    }

}
