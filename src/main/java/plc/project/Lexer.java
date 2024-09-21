package plc.project;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    public List<Token> lex() {
        List<Token> tokens = new ArrayList<Token>();
        while (peek(".")) {
            if (peek("[ \b\n\r\t]")) {
                chars.advance();
                chars.skip();
            }
            else {
                tokens.add(lexToken());
            }
        }
        return tokens;
    }

    public Token lexToken() {
        if (peek("[A-Za-z_]")) {
            return lexIdentifier();
        }
        else if (peek("[+\\-]", "[0-9]") || peek("[0-9]")) {
            return lexNumber();
        }
        else if (peek("\'")) {
            return lexCharacter();
        }
        else if (peek("\"")) {
            return lexString();
        }
        else {
            return lexOperator();
        }
    }

    public Token lexIdentifier() {
        // keep advancing our stream until the pattern is no more for our identifier
        while (peek("[A-Za-z_0-9-]")) {
            match("[A-Za-z_0-9-]");
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        // Check for a positive or minus sign first
        if (peek("[+\\-]")) {
            match("[+\\-]");
        }
        // Check for leading zeros and handle them
        if (peek("0")) {
            match("0");
            // If we see a digit after the leading zero, it's an invalid integer (unless it's a decimal)
            if (peek("\\d")) {
                throw new ParseException("Invalid integer with leading zeros", chars.index);
            }
        } else {
            // Grab digits for the integer part before the decimal point
            while (peek("\\d")) {
                match("\\d");
            }
        }
        // Check if it's a decimal
        if (peek("[.]", "\\d")) {
            match("[.]");
            while (peek("\\d")) {
                match("\\d");
            }
            return chars.emit(Token.Type.DECIMAL);
        }

        return chars.emit(Token.Type.INTEGER);
    }


    public Token lexCharacter() {
        // advance on the stream with match
        if (peek("\'")) {
            match("\'");
        }
        //
        if (peek("\\\\")) {
            lexEscape();
        }
        else if (peek("[^\'\\n\\r\\\\]"))  {
            match("[^\'\\n\\r\\\\]");
        }
        else {
            throw new ParseException("Illegal Character", chars.index);
        }
        if (peek("\'")) {
            match("\'");
            return chars.emit(Token.Type.CHARACTER);
        }
        throw new ParseException("this is not a valid character", chars.index);
    }

    public Token lexString() {
        // advance on our open quote
        if (peek("\"")) {
            match("\"");
        }
        // go until we have a closed quote, new line, or a carriage return /r
        while (peek("[^\"\\n\\r]")) {
            // if we have an escape handle it
            if (peek("\\\\")) {
                // advance on an escape
                lexEscape();
            }
            else {
                // match any character
                match(".");
            }
        }
        // make sure we find the closing quote and add it
        if (peek("\"")) {
            match("\"");
        }
        else {
            // if we never came across the closing quote or had a carriage return or new line before it this will execute
            match(".");
            throw new ParseException("Unterminated String", chars.index);
        }
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        // we peak in lexToken, then peek again here and if that passes we act on it with match (advance the stream)
        if (peek("\\\\", "[bnrt\'\"\\\\]")){
            match("\\\\", "[bnrt\'\"\\\\]");
        }
        else {
            match(".");
            throw new ParseException("not a possible escape", chars.index);
        }
    }

    public Token lexOperator() {
        if (peek("[<>!=]", "=")) {
            match("[<>!=]", "=");
        }
        else {
            match(".");
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (  !chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        if (peek(patterns)) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return true;
    }


    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

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



// try using the lex escape