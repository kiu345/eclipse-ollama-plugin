package com.github.kiu345.eclipse.eclipseai.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.kiu345.eclipse.eclipseai.part.ChatViewPart;
import com.github.kiu345.eclipse.eclipseai.part.helper.UIInputHandler;

class MessageParserTest {
    private static final String TEST1 = "<p>Leo's chest ached. \"How do I fix it?\"</p><br/>2>1";
    private static final String TEST2 = "Leo's chest ached. \"How do I fix it?\"\\\n2>1";

    private static final String TEST3 = """
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

    private static final String TEST4 = """
            <div class="boat">
                <p>Curious, Leo followed the fox deep into the woods. The trees grew taller, their branches weaving a canopy of twilight.</p>
                <p>The fox led him to a clearing where a child's toy boat floated on a pond of starlight.</p>
                <p>The boat was frozen, its sail tattered.</p>
            </div>""";
    private static final String TEST5 = """
            <think>
            Okay, the user asked for a unit test for the Blubb class. I need to generate a test class using JUnit. Let me check the existing code. The Blubb class has a main method but no actual functionality. So the test should probably focus on any methods that might be added later.

            Wait, the current Blubb class doesn't have any methods besides main. So maybe the user is planning to add some functionality. The test template I provided earlier uses JUnit 5, which is good. But since there's no code to test yet, the test method is empty.

            I should make sure the test class is in the same package, which it is. The user might need to add assertions or setup code. Maybe suggest adding a test for the main method, but since it's static, they might need to use a test framework that supports that. Alternatively, if the Blubb class is supposed to have other methods, the test should reflect that.

            Wait, the user's original code has a main method with a TODO. Maybe they intended to have some logic there. But as it stands, there's nothing to test. So the unit test is a placeholder. I should inform the user that since the Blubb class is empty, the test is also a template and needs to be filled with actual test cases once the functionality is implemented.

            Also, make sure the test class is named BlubbTest and is in the test package if following standard Maven structure. But the current code has the package as test, which might not be ideal. Maybe suggest changing the package to test if it's part of a test suite, but that's beyond the current scope.

            In any case, the provided test class is a starting point. The user can expand it once they add methods to Blubb. So the response should present the test code and note that it's a template needing further details.
            </think>

            Here's a JUnit 5 unit test template for your `Blubb` class:

            ```java
            package test;

            import static org.junit.jupiter.api.Assertions.*;
            import org.junit.jupiter.api.Test;

            public class BlubbTest {

                @Test
                public void testBlubbClass() {
                    // TODO: Add test logic here
                    // Example: assertEquals(expected, actual)
                }
            }
            ```

            **Notes:**
            1. This test class is currently empty since the `Blubb` class has no implemented functionality
            2. You'll need to add actual test cases when you implement the `Blubb` class
            3. The test uses JUnit 5's modern API (`@Test` annotation and `Assertions` class)

            Would you like me to add specific test cases based on potential functionality you plan to implement?
            .""";

    private static final String TEST6 = """
            <think>
            Okay, the user asked for a unit test for the Blubb class. I need to generate a test class using JUnit. Let me check the existing code. The Blubb class has a main method but no actual functionality. So the test should probably focus on any methods that might be added later.
            </think>

            Here's a JUnit 5 unit test template for your `Blubb` class:

            ```java
            public class BlubbTest {

                @Test
                public void testBlubbClass() {
                }
            }
            ```

            **Notes:**
            1. This test class is currently empty since the `Blubb` class has no implemented functionality""";

    private MessageParser parser;

    @Test
    void testParseToHtml1() {
        System.out.println("MessageParserTest.testParseToHtml1()");
        parser = new MessageParser();
        String result = parser.parseToHtml(UUID.randomUUID(), TEST1);
//      System.out.println(result);
        assertThat(result)
                .isNotNull()
                .describedAs("< and > should be escaped in content")
                .matches("[\\s]*<p>[^<>]*</p>[\\s]*")
                .describedAs("No newline should exist")
                .doesNotContain("<br/>", "<br />");
    }

    @Test
    void testParseToHtml2() {
        System.out.println("MessageParserTest.testParseToHtml2()");
        System.out.println(TEST2 + "\n---");
        parser = new MessageParser();
        String result = parser.parseToHtml(UUID.randomUUID(), TEST2);
//      System.out.println(result);
        assertThat(result)
                .isNotNull()
                .containsAnyOf("<br/>", "<br />");
    }

    @Test
    void testParseToHtml3() {
        System.out.println("MessageParserTest.testParseToHtml3()");
        System.out.println(TEST3 + "\n---");
        parser = new MessageParser();
        String result = parser.parseToHtml(UUID.randomUUID(), TEST3);
        assertNotNull(result);
        System.out.println(result);
    }

    @Test
    void testParseToHtmlWithOtherFunctions() {
        System.out.println("MessageParserTest.testParseToHtmlWithOtherFunctions()");
        System.out.println(TEST4 + "\n---");
        parser = new MessageParser();
        String result = UIInputHandler.escapeHtmlQuotes(UIInputHandler.fixLineBreaks(parser.parseToHtml(UUID.randomUUID(), TEST4)));
        assertNotNull(result);
        System.out.println(result);
    }

    @Test
    void testParseToHtml5() {
        System.out.println("MessageParserTest.testParseToHtml5()");
        System.out.println(TEST5 + "\n---");
        parser = new MessageParser();
        String result = parser.parseToHtml(UUID.randomUUID(), TEST5);
        assertNotNull(result);
        System.out.println(result);
    }

    @Test
    void testParseToHtml6() {
//        System.out.println("MessageParserTest.testParseToHtml6()");
//        System.out.println(TEST6 + "\n---");
        parser = new MessageParser();
        String result = parser.parseToHtml(UUID.randomUUID(), TEST6);
        assertNotNull(result);
        System.out.println("----");
        System.out.println(result);
        System.out.println("----");
    }

    @Test
    void testEscapeBackSlashes() {
        System.out.println("MessageParserTest.testEscapeBackSlashes()");
        String result = MessageParser.escapeBackSlashes("\\b");
        assertNotNull(result);
        assertEquals("\\\\b", result);
    }

    @Test
    void testMarkdown() {
        System.out.println("MessageParserTest.testMarkdown()");
        String result = MessageParser.markdown("*bold*");
        assertNotNull(result);
        assertEquals("<p><em>bold</em></p>", result.trim());
    }

}
