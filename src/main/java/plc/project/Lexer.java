package plc.project;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */


public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();

        while (chars.has(0)){
            if (peek(" ", "\b", "\n", "\r", "\t")) {  // Check for all whitespace characters
                // If the current character is a whitespace, advance past it
                // ONLY ADVANCE ONCE
                chars.advance();
            } else {
                // lextoken will add a token. indexing will be taken care of inside that function
                tokens.add(lexToken());
            }
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (match("[A-Za-z_][A-Za-z0-9_-]*")) {
            return lexIdentifier();
        }
        else if (match("'0'|[+-]?[1-9][0-9]*")) {
            // integer? TODO
            return lexNumber();  // lexNumber handles both integers and decimals
        }
        else if (match("[+-]?(0|[1-9][0-9]*)\\.[0-9]+")) {
            // decimal? TODO
            return lexNumber();  // lexNumber handles both integers and decimals
        }
        else if (match("[']([^'\\\\]|\\\\[bnrt'\"])[']\n")) {
            return lexCharacter();
        }
        else if (match("\"([^\"\\n\\r\\\\]|\\\\[bnrt'\"])*\"\n")) {
            return lexString();
        }
        else if (match("[<>!=]=?|&&|\\|\\||.\n")) {
            return lexOperator();
        }
        // If no valid token is found, throw a ParseException
        else {
            throw new ParseException("Unexpected character or invalid token", chars.index);
        }
    }


    public Token lexIdentifier() {
        // at this point, the match function has already advanced the stream
        // we can directly emit the identifier token based on the current state
        // repeat for other Token return type functions
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        // either the integer or decimal number in the `match()` so check for a .
        if (chars.input.contains(".")) {
            return chars.emit(Token.Type.DECIMAL); // If there's a decimal point, it's a DECIMAL
        } else {
            return chars.emit(Token.Type.INTEGER); // Otherwise, it's an INTEGER
        }
    }

    public Token lexCharacter() {
        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */

    public String get_next_string () {
        //grab a substring up until the next space
        StringBuilder current = new StringBuilder();  // Use StringBuilder for efficient concatenation
        int iterate_index = 0;

        while (iterate_index + chars.index < chars.length && chars.has(iterate_index)) {
            char currentChar = chars.get(iterate_index);
            if (currentChar == ' ' || currentChar == '\b' || currentChar == '\n' || currentChar == '\r' || currentChar == '\t') {
                break;
            }
            current.append(currentChar);
            iterate_index++;
        }

        // check our resulting substring against a regex pattern
        return current.toString();
    }

    public boolean peek(String... patterns) {
        // try all the patterns to see if any match
        String next_string = get_next_string();
        chars.set_next_string_length(next_string.length());

        for (String pattern : patterns) {
            Pattern regexPattern = Pattern.compile(pattern);

            Matcher matcher = regexPattern.matcher(next_string);

            // if we find a match return true
            if (matcher.matches()) {
                return true;
            }
        }

        return false;
    }
    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        for (String pattern : patterns) {
            // we can just call peek, only throw one pattern at a time so we can identify which one matches
            // when we have matching advance by the length of pattern
            if (peek(pattern)) {

                for (int i = 0; i < chars.next_string_length; i++) {
                    chars.advance();
                }
                chars.reset_next_string_length();
                return true;
            }
        }
        return false;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;
        private int next_string_length = 0;

        public void set_next_string_length(int length) {
            this.next_string_length = length;
        }

        public void reset_next_string_length() {
            this.next_string_length = 0;
        }

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }
    }
}
