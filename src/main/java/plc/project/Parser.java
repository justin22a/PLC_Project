package plc.project;
import java.math.BigDecimal;
import java.math.BigInteger;
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

    private boolean parsingIncrement = false;

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
        while (tokens.has(0)) {
            if (peek("LET")) {
                fields.add(parseField());
            } else if (peek("DEF")) {
                methods.add(parseMethod());
            } else {
                throw new ParseException("Expected field or method declaration", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
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

        // Parse field name
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected field name after 'LET'", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);

        // Parse optional type
        String type = "Any"; // Default type
        if (match(":")) {
            if (!peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected type after ':'", tokens.get(0).getIndex());
            }
            type = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        }

        // Parse optional initialization value
        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            throw new ParseException("Expected ';' at end of field declaration.", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
        }

        return new Ast.Field(name, type, false, value);
    }



    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        if (!match("DEF")) {
            throw new ParseException("Expected 'DEF'", tokens.get(0).getIndex());
        }

        String name = nextIdentifierWithCheck("Expected method name");

            if (!match("(")) {
            throw new ParseException("Expected '('", tokens.get(0).getIndex());
        }

        List<String> parameters = new ArrayList<>();
        List<String> parameterTypeNames = new ArrayList<>();

            while (!peek(")")) {
            parameters.add(nextIdentifierWithCheck("Expected parameter name"));

            if (!match(":")) {
                throw new ParseException("Expected ':' after parameter name", tokens.get(0).getIndex());
            }

                parameterTypeNames.add(nextIdentifierWithCheck("Expected parameter type"));

            if (!match(",") && !peek(")")) {
                throw new ParseException("Expected ',' or ')'", tokens.get(0).getIndex());
            }
        }
        match(")");

        String returnTypeName = parseOptionalReturnType();

            expect("DO", "Expected 'DO' to start method body");

        List<Ast.Statement> statements = parseStatementsUntil("END");

            return new Ast.Method(name, parameters, parameterTypeNames, Optional.of(returnTypeName), statements);
    }

    private String nextIdentifierWithCheck(String errorMessage) throws ParseException {
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException(errorMessage, tokens.get(0).getIndex());
        }
        String identifier = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);
            return identifier;
        }

    private String parseOptionalReturnType() throws ParseException {
        if (peek(":")) {
                match(":");
            return nextIdentifierWithCheck("Expected return type");
        }
        return "Any";
    }

    private void expect(String expected, String errorMessage) throws ParseException {
        if (    !match(expected)) {
            throw new ParseException(errorMessage, tokens.get(0).getIndex());
        }
    }

    private List<Ast.Statement> parseStatementsUntil(String terminal) throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek(terminal)) {
            statements.add(parseStatement());
        }
        match(terminal);
        return statements;
    }


    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET")) {
            return parseDeclarationStatement();
        } else if (peek("IF")) {
            return parseIfStatement();
        } else if (peek("FOR")) {
            return parseForStatement();
        } else if (peek("WHILE")) {
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            return parseReturnStatement();
        } else {
            return parseExpressionOrAssignment();
        }
    }

    private Ast.Statement parseExpressionOrAssignment() throws ParseException {
        Ast.Expression expr = parseExpression();  // Parse the left-hand side of the assignment or a standalone expression

        if (match("=")) {
            // If it's an assignment, expect another valid expression after '='
            if (!tokens.has(0)) {
                throw new ParseException("Expected expression after '='", -1);  // If no more tokens, handle gracefully
            }
            Ast.Expression valueExpr = parseExpression();  // Parse the right-hand side of the assignment
            if (!parsingIncrement) {
                expectSemicolon();  // Ensure there is a semicolon after the assignment...unless parsing an increment statement
            }
            return new Ast.Statement.Assignment(expr, valueExpr);
        } else {
            expectSemicolon();  // Ensure there is a semicolon after the expression
            return new Ast.Statement.Expression(expr);
        }
    }

    private void expectSemicolon() throws ParseException {
        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.has(0) ? tokens.get(0).getIndex() : -1);
        }
    }


    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);

        Optional<String> type = Optional.empty();
        if (match(":")) {
            type = Optional.of(tokens.get(0).getLiteral());
            match(Token.Type.IDENTIFIER);
        }

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            throw new ParseException("Expected ';' at end of declaration.", tokens.get(0).getIndex());
        }

        return new Ast.Statement.Declaration(name, type, value);
    }
    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        match("IF");
        List<Ast.Statement> thenStatements = new ArrayList<>();
        List<Ast.Statement> elseStatements = new ArrayList<>();
        Ast.Expression condition = parseExpression();
        if (!match("DO")) {
            throw new ParseException("Expected 'DO'", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
        }
        // Loop to capture then statments
        while(!peek("ELSE") && !peek("END")) {
            thenStatements.add(parseStatement());
        }
        if (match("END")) {
            return new Ast.Statement.If(condition, thenStatements, elseStatements);
        }
        if (!match("ELSE")) {
            throw new ParseException("Expected 'ELSE'", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
        }
        // Loop to capture else statments
        while(!peek("END")) {
            elseStatements.add(parseStatement());
        }
        if (!match("END")) {
            throw new ParseException("Expected 'END'", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
        }
        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }
    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        match("FOR");

        if (!match("(")) {
            throw new ParseException("Expected '(' after 'FOR'", tokens.get(0).getIndex());
        }

        Ast.Statement init = null;
        if (!peek(";")) {
            parsingIncrement = true;  // Temporarily disable semicolon enforcement for initialization
            init = parseExpressionOrAssignment();  // Could be an expression or an assignment
            parsingIncrement = false;
            if (!match(";")) {
                throw new ParseException("Expected ';' after initialization part of for loop", tokens.get(0).getIndex());
            }
        } else {
            match(";");  // Handle the empty initialization
        }

        Ast.Expression condition = null;
        if (!peek(";")) {
            condition = parseExpression();  // Parse the condition part of the for loop
            if (!match(";")) {
                throw new ParseException("Expected ';' after condition in for loop", tokens.get(0).getIndex());
            }
        } else {
            match(";");  // Handle the empty condition
        }

        Ast.Statement increment = null;
        if (!peek(")")) {
            parsingIncrement = true;  // Temporarily disable semicolon enforcement for increment
            increment = parseExpressionOrAssignment();  // Could be an expression or an assignment
            parsingIncrement = false;
        }
        if (!match(")")) {
            throw new ParseException("Expected ')' after for loop parameters", tokens.get(0).getIndex());
        }

        expect("DO", "Expected 'DO' to start the body of the for loop");

        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        match("END");

        return new Ast.Statement.For(init, condition, increment, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");
        List<Ast.Statement> statements = new ArrayList<>();
        Ast.Expression condExpression = parseExpression();
        if (!match("DO")) {
            throw new ParseException("Expected 'DO'", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
        }
        if (!peek("END")) {
            while (tokens.has(0) && !peek("END")) { // Ensure there are tokens and not at 'END'
                statements.add(parseStatement());
            }
        }
        if (!match("END")) {
            throw new ParseException("Expected 'END'", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
        }
        return new Ast.Statement.While(condExpression, statements);
    }
    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match("RETURN");
        Ast.Expression returnVal = parseExpression();
        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
        }
        return new Ast.Statement.Return(returnVal);
    }
    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }
    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression result = parseComparisonExpression();
        while (peek("&&") || peek("||")) {
            String operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            Ast.Expression right = parseComparisonExpression();
            result = new Ast.Expression.Binary(operator, result, right);
        }
        return result;
    }

    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression result = parseAdditiveExpression();
        while (peek("<") || peek("<=") || peek(">") || peek(">=") || peek("==") || peek("!=")) {
            String operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            Ast.Expression right = parseAdditiveExpression();
            result = new Ast.Expression.Binary(operator, result, right);
        }
        return result;
    }
    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression result = parseMultiplicativeExpression();
        while (peek("+") || peek("-")) {
            String operator = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression right = parseMultiplicativeExpression();
            result = new Ast.Expression.Binary(operator, result, right);
        }
        return result;
    }

    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression result = parseSecondaryExpression();
        while (peek("*") || peek("/")) {
            String operator = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression right = parseSecondaryExpression();
            result = new Ast.Expression.Binary(operator, result, right);
        }
        return result;
    }
    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression result = parsePrimaryExpression();
        while (peek(".")) {
            match(".");
            String member = tokens.get(0).getLiteral();
            if (!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected an identifier", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
            }

            if (peek("(")) {
                match("(");
                List<Ast.Expression> arguments = new ArrayList<>();
                if (!peek(")")) {
                    arguments.add(parseExpression());
                    while (match(",")) {
                        if (peek(")")) { // Check for trailing comma
                            throw new ParseException("Unexpected trailing comma in function arguments", tokens.get(0).getIndex());
                        }
                        arguments.add(parseExpression());
                    }
                }
                if (!match(")")) {
                    throw new ParseException("Expected ')'", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
                }
                result = new Ast.Expression.Function(Optional.of(result), member, arguments);
            } else {
                result = new Ast.Expression.Access(Optional.of(result), member);
            }
        }
        return result;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (peek("NIL")) {
            match("NIL");
            return new Ast.Expression.Literal(null);
        } else if (peek("TRUE")) {
            match("TRUE");
            return new Ast.Expression.Literal(Boolean.TRUE);
        } else if (peek("FALSE")) {
            match("FALSE");
            return new Ast.Expression.Literal(Boolean.FALSE);
        } else if (peek(Token.Type.INTEGER)) {
            String literal = tokens.get(0).getLiteral();
            match(Token.Type.INTEGER);
            return new Ast.Expression.Literal(new BigInteger(literal));
        } else if (peek(Token.Type.DECIMAL)) {
            String literal = tokens.get(0).getLiteral();
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(new BigDecimal(literal));
        } else if (peek(Token.Type.CHARACTER)) {
            String literal = tokens.get(0).getLiteral();
            match(Token.Type.CHARACTER);
            String noQuotes = literal.substring(1, literal.length() - 1)
                    .replace("\\b", "\b")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            return new Ast.Expression.Literal(noQuotes.charAt(0));
        } else if (peek(Token.Type.STRING)) {
            String literal = tokens.get(0).getLiteral();
            match(Token.Type.STRING);
            String noQuotes = literal.substring(1, literal.length() - 1)
                    .replace("\\b", "\b")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            return new Ast.Expression.Literal(noQuotes);
        } else if (match("(")) {
            // Parse expression inside parentheses
            if (!tokens.has(0)) {
                throw new ParseException("Expected expression after '(', but reached end of input", tokens.get(-1).getIndex());
            }
            Ast.Expression expr = parseExpression();

            // Ensure the closing parenthesis exists
            if (!match(")")) {
                int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex();
                throw new ParseException("Expected ')' after expression", errorIndex);
            }
            return new Ast.Expression.Group(expr);
        } else if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);

            if (match("(")) { // Parse function call
                List<Ast.Expression> arguments = new ArrayList<>();
                if (!peek(")")) {
                    arguments.add(parseExpression());
                    while (match(",")) {
                        arguments.add(parseExpression());
                    }
                }
                if (!match(")")) {
                    int errorIndex = tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex();
                    throw new ParseException("Expected ')' at end of function arguments", errorIndex);
                }
                return new Ast.Expression.Function(Optional.empty(), name, arguments);
            } else {
                return new Ast.Expression.Access(Optional.empty(), name);
            }
        } else {
            throw new ParseException("Invalid primary expression", tokens.has(0) ? tokens.get(0).getIndex() : tokens.get(-1).getIndex());
        }
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