package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import plc.project.Ast.Statement;

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
        while (tokens.has(0)) {
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
        if (!match("(")) {
            throw new ParseException("Expected '('", tokens.get(0).getIndex());
        }
        // Capture parameters
        if (!peek(")")) {
            // Catch the first identifier
            if (!peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected an identifier after '('", tokens.get(0).getIndex());
            }
            parameters.add(tokens.get(0).getLiteral());
            match(Token.Type.IDENTIFIER);
            // Loop for any additional identifiers
            while(match(",")) {
                if (!peek(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected an identifier after ','", tokens.get(0).getIndex());
                }
                parameters.add(tokens.get(0).getLiteral());
                match(Token.Type.IDENTIFIER);
            }

        }
        if (!match(")")) {
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
        if (match("LET")) {
            return parseDeclarationStatement();
        }
        else if (match("IF")) {
            return parseIfStatement();
        }
        else if (match("FOR")) {
            return parseForStatement();
        }
        else if (match("WHILE")) {
            return parseWhileStatement();
        }
        else if (match("RETURN")) {
            return parseReturnStatement();
        }
        else {
            Ast.Expression expr = parseExpression();
            if (match("=")) {
                Ast.Expression optExpr = parseExpression();
                if ((match(";"))) {
                    return new Ast.Statement.Assignment(expr, optExpr);
                }
                else {
                    throw new ParseException("Expected ';'", tokens.get(0).getIndex());
                }
            }
            return new Ast.Statement.Expression(expr);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected an identifier", tokens.get(0).getIndex());
        }
        String varName = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);
        if (match(";")) {
            return new Ast.Statement.Declaration(varName, Optional.empty());
        }
        if (!match("=")) {
            throw new ParseException("Expected an '='", tokens.get(0).getIndex());
        }
        Ast.Expression varValue = parseExpression();
        if (!match(";")) {
            throw new ParseException("Expected a ';'", tokens.get(0).getIndex());
        }
        return new Ast.Statement.Declaration(varName, Optional.of(varValue));
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        List<Ast.Statement> thenStatements = new ArrayList<>();
        List<Ast.Statement> elseStatements = new ArrayList<>();
        Ast.Expression condition = parseExpression();
        if (!match("DO")) {
            throw new ParseException("Expected 'DO'", tokens.get(0).getIndex());
        }
        // Loop to capture then statments
        while(!peek("ELSE") && !peek("END")) {
            thenStatements.add(parseStatement());
        }
        if (match("END")) {
            return new Ast.Statement.If(condition, thenStatements, elseStatements);
        }
        if (!match("ELSE")) {
            throw new ParseException("Expected 'ELSE'", tokens.get(0).getIndex());
        }
        // Loop to capture else statments
        while(!peek("END")) {
            elseStatements.add(parseStatement());
        }
        if (!match("END")) {
            throw new ParseException("Expected 'END'", tokens.get(0).getIndex());
        }
        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        Ast.Statement initStatement = null;
        Ast.Expression condExpression;
        Ast.Statement incrStatement = null;
        List<Ast.Statement> statements = new ArrayList<>();
        if (!match("(")) {
            throw new ParseException("Expected '('", tokens.get(0).getIndex());
        }
        if (!match(";")) { // Initialization statement is present...parse it
            if (!peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected an identifier", tokens.get(0).getIndex());
            }
            String firstIdent = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (!match("=")) {
                throw new ParseException("Expected an '='", tokens.get(0).getIndex());
            }
            Ast.Expression firstExpr = parseExpression();
            initStatement = new Ast.Statement.Declaration(firstIdent, Optional.of(firstExpr));
            if (!match(";")) { // Missing semicolon after initialization statement...throw an exception
                throw new ParseException("Expected a ';'", tokens.get(0).getIndex());
            }
        }
        if (match(";")) { // Conditional expression is not present...throw an exception
            throw new ParseException("Expected an expression", tokens.get(0).getIndex());
        }
        // Parse the second statement
        condExpression = parseExpression();
        if (!match(";")) { // Missing semicolon after conditional expression...throw an exception
            throw new ParseException("Expected a ';'", tokens.get(0).getIndex());
        }
        if (!match(";")) { // Incremenentation statement is present...parse it
            if (!peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected an identifier", tokens.get(0).getIndex());
            }
            String lastIdent = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (!match("=")) {
                throw new ParseException("Expected an '='", tokens.get(0).getIndex());
            }
            Ast.Expression lastExpr = parseExpression();
            incrStatement = new Ast.Statement.Declaration(lastIdent, Optional.of(lastExpr));
            if (!match(")")) { // Missing closing parentheses after incremenentation statement...throw an exception
                throw new ParseException("Expected a ')'", tokens.get(0).getIndex());
            }
        }
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        if (!match("END")) {
            throw new ParseException("Expected 'END'", tokens.get(0).getIndex());
        }
        return new Ast.Statement.For(initStatement, condExpression, incrStatement, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();
        Ast.Expression condExpression = parseExpression();
        if (!match("DO")) {
            throw new ParseException("Expected 'DO'", tokens.get(0).getIndex());
        }
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        if (!match("END")) {
            throw new ParseException("Expected 'END'", tokens.get(0).getIndex());
        }
        return new Ast.Statement.While(condExpression, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        Ast.Expression returnVal = parseExpression();
        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.get(0).getIndex());
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



    /**
     * Parses the {@code comparison-expression} rule.
     */
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
            match(Token.Type.IDENTIFIER);

            if (peek("(")) {
                match("(");
                List<Ast.Expression> arguments = new ArrayList<>();
                if (!peek(")")) {
                    arguments.add(parseExpression());
                    while (match(",")) {
                        arguments.add(parseExpression());
                    }
                }
                if (!match(")")) {
                    throw new ParseException("Expected ')'", tokens.get(0).getIndex());
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
            String lit = tokens.get(0).getLiteral();
            match(Token.Type.INTEGER);
            return new Ast.Expression.Literal(new BigInteger(lit));
        } else if (peek(Token.Type.DECIMAL)) {
            String lit = tokens.get(0).getLiteral();
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(new BigDecimal(lit));
        } else if (peek(Token.Type.CHARACTER)) {
            String lit = tokens.get(0).getLiteral();
            match(Token.Type.CHARACTER);
            String noQuotes = lit.substring(1, lit.length() - 1);
            noQuotes = noQuotes
                    .replace("\\b", "\b")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            return new Ast.Expression.Literal(noQuotes.charAt(0));
        } else if (peek(Token.Type.STRING)) {
            String lit = tokens.get(0).getLiteral();
            match(Token.Type.STRING);
            String noQuotes = lit.substring(1, lit.length() - 1);
            noQuotes = noQuotes
                    .replace("\\b", "\b")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            return new Ast.Expression.Literal(noQuotes);
        } else if (match("(")) {
            Ast.Expression expr = parseExpression();
            if (!match(")")) {
                throw new ParseException("Expected ')' after expression", tokens.get(0).getIndex());
            }
            return new Ast.Expression.Group(expr);
        } else if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (match("(")) {
                List<Ast.Expression> arguments = new ArrayList<>();
                if (!peek(")")) {
                    arguments.add(parseExpression());
                    while (match(",")) {
                        arguments.add(parseExpression());
                    }
                }
                match(")");
                return new Ast.Expression.Function(Optional.empty(), name, arguments);
            } else {
                return new Ast.Expression.Access(Optional.empty(), name);
            }
        } else {
            throw new ParseException("Invalid primary expression", tokens.get(0).getIndex());
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
