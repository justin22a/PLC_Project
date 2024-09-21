package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIntegerEdgeCases(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testIntegerEdgeCases() {
        return Stream.of(
                Arguments.of("Negative Zero", "-0", true),
                Arguments.of("Positive Zero", "+0", true),
                Arguments.of("Zero with Trailing Zero", "0", true),
                Arguments.of("Leading Negative with Trailing Zero", "-1000", true),
                Arguments.of("Leading Positive with Trailing Zero", "+5000", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimalEdgeCases(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimalEdgeCases() {
        return Stream.of(
                Arguments.of("Zero Decimal", "0.0", true),
                Arguments.of("Negative Zero Decimal", "-0.0", true),
                Arguments.of("Positive Decimal with Trailing Zeros", "100.00", true),
                Arguments.of("Negative Decimal with Leading Digit", "-5.25", true),
                Arguments.of("Valid Decimal", "3.14159", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testStringEdgeCases(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testStringEdgeCases() {
        return Stream.of(
                Arguments.of("String with Escape Sequence", "\"Hello\\tWorld!\"", true),
                Arguments.of("String with Quotes", "\"She said, \\\"Hello!\\\"\"", true),
                Arguments.of("Empty String", "\"\"", true),
                Arguments.of("Unterminated String with Escape", "\"Hello\\nWorld", false),
                Arguments.of("Invalid Escape Sequence", "\"Hello\\xWorld\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacterEdgeCases(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacterEdgeCases() {
        return Stream.of(
                Arguments.of("Valid Character", "'a'", true),
                Arguments.of("Valid Escape Character", "'\\n'", true),
                Arguments.of("Empty Character", "''", false),
                Arguments.of("Multiple Characters", "'abc'", false),
                Arguments.of("Invalid Escape Sequence", "'\\x'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperatorEdgeCases(String test, String input, boolean success) {
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperatorEdgeCases() {
        return Stream.of(
                Arguments.of("Single Character Operator", "+", true),
                Arguments.of("Single Character Operator", "-", true),
                Arguments.of("Complex Operator", "==", true),
                Arguments.of("Invalid Whitespace as Operator", " ", false),
                Arguments.of("Complex Operator", "&&", true),
                Arguments.of("Complex Operator", "||", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Leading Zero", "01", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Equal", "=", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3 (CUSTOM)", "if num!=5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "if", 0),
                        new Token(Token.Type.IDENTIFIER, "num", 3),
                        new Token(Token.Type.OPERATOR, "!=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 4 (CUSTOM)", "while(num!=5){return 0;}", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "while", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.IDENTIFIER, "num", 6),
                        new Token(Token.Type.OPERATOR, "!=", 9),
                        new Token(Token.Type.INTEGER, "5", 11),
                        new Token(Token.Type.OPERATOR, ")", 12),
                        new Token(Token.Type.OPERATOR, "{", 13),
                        new Token(Token.Type.IDENTIFIER, "return", 14),
                        new Token(Token.Type.INTEGER, "0", 21),
                        new Token(Token.Type.OPERATOR, ";", 22),
                        new Token(Token.Type.OPERATOR, "}", 23)
                )),
                Arguments.of("Example 5 (CUSTOM)", "\"string\"", Arrays.asList(
                        new Token(Token.Type.STRING, "\"string\"", 0)
                )),
                Arguments.of("Example 6 (CUSTOM)", "\"\"", Arrays.asList(
                        new Token(Token.Type.STRING, "\"\"", 0)
                )),
                Arguments.of("Example 7 (CUSTOM)", "\"str\\ning\"", Arrays.asList(
                        new Token(Token.Type.STRING, "\"str\\ning\"", 0)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
