package plc.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling those functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();
        while (tokens.has(0)) { // While there are more tokens
            if (peek("LET")) {
                fields.add(parseField());
            } else if (peek("DEF")) {
                methods.add(parseMethod());
            } else {
                throw new ParseException("Expected field or method declaration", tokens.get(0).getIndex());
            }
        }
        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        match("LET");

        // handle const scenario
        boolean constant = peek("CONST");
        if (constant) {
            match("CONST");
        }

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier for field name.", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + 1);
        }
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);

        String typeName = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);

        Optional<Ast.Expression> value = Optional.empty();
        if (peek("=")) {
            match("=");
            value = Optional.of(parseExpression());
        }

        if (!peek(";")) {
            throw new ParseException("Expected ';' at end of field declaration.", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex() + 1);
        }
        match(";");

        return new Ast.Field(name, constant, value);
    }





    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected an identifier", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);

        List<String> parameters = new ArrayList<>();
        List<Ast.Statement> statements = new ArrayList<>();
        if (!match('(')) {
            throw new ParseException("Expected '('", tokens.get(0).getIndex());  
        }
        // Capture parameters
        if (!peek(')')) {
            // Catch the first identifier
            if (!peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected an identifier after '('", tokens.get(0).getIndex());
            }
            parameters.add(tokens.get(0).getLiteral());
            match(Token.Type.IDENTIFIER);
            // Loop for any additional identifiers
            while(match(',')) {
                if (!peek(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected an identifier after ','", tokens.get(0).getIndex());
                }
                parameters.add(tokens.get(0).getLiteral());
                match(Token.Type.IDENTIFIER);
            }
            
        }
        if (!match(')')) {
            throw new ParseException("Expected ')'", tokens.get(0).getIndex());  
        }
        if (!match("DO")) {
            throw new ParseException("Expected 'DO'", tokens.get(0).getIndex());  
        }
        // Loop to capture statments
        while(!peek("END")) {
            statements.add(parseStatement());
        }
        if (!match("END")) {
            throw new ParseException("Expected 'END'", tokens.get(0).getIndex());  
        }
        return new Ast.Method(name, parameters, statements);         
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        Ast.Expression expr = parseExpression();
        if (match('=')) {
            Ast.Expression optExpr = parseExpression();
            if ((match(';'))) {
                return new Ast.Statement.Assignment(expr, optExpr);
            }
            else {
                throw new ParseException("Expected '\''", tokens.get(0).getIndex());
            }
        }
        return new Ast.Statement.Expression(expr);
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO -> NOT IN PART A
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO -> NOT IN PART A
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO -> NOT IN PART A
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO -> NOT IN PART A
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO -> NOT IN PART A
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    public boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    public boolean match(Object... patterns) {
        boolean peek = peek(patterns);
    
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
    
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
