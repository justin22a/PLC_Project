package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {

        //  Opener
        writer.write("public class Main {");
        newline(0);
        ++indent;

        //  Fields
        for (int i = 0; i < ast.getFields().size(); i++) {
            newline(indent);
            visit(ast.getFields().get(i));
            if (i == ast.getFields().size() - 1) {
                newline(0);
            }
        }

        // Pub stat void main stuff
        newline(indent);
        writer.write("public static void main(String[] args) {");
        ++indent;
        newline(indent);
        writer.write("System.exit(new Main().main());");
        --indent;
        newline(indent);
        writer.write("}");
        newline(0);

        // Methods
        for (int i = 0; i < ast.getMethods().size(); i++) {
            newline(indent);
            visit(ast.getMethods().get(i));
            if (i < ast.getMethods().size() - 1) {
                newline(0);
            }
        }

        // Closer
        newline(0);
        newline(0);
        writer.write("}");

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {

        // Final?
        if (ast.getConstant()) {
            writer.write("final ");
        }

        // Type and name
        writer.write(ast.getTypeName() + " " + ast.getName());

        // Set to initial value?
        if (ast.getValue().isPresent()) {
            writer.write(" = ");
            visit(ast.getValue().get());
        }

        // Ending semicolon
        writer.write(";");

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {

        // Opening return type, name, and parentheses
        writer.write(ast.getFunction().getReturnType().getJvmName() + " " + ast.getName() + "(");

        // Comma-separated list of parameters with types
        for (int i = 0; i < ast.getParameters().size(); i++) {
            writer.write(ast.getFunction().getParameterTypes().get(i).getJvmName()); // TODO: or this..? --> writer.write(ast.getParameterTypeNames().get(i));
            writer.write(" ");
            writer.write(ast.getParameters().get(i));
            if (i < ast.getParameters().size() - 1) {
                writer.write(", ");
            }
        }
        // Closing parentheses and opening brace
        writer.write(") {");

        // Close brace on same line if there's no statements
        if (ast.getStatements().isEmpty()) {
            writer.write("}");
        }
        // Otherwise, print each statement and close braces on a new line at the end
        else {
            for (Ast.Statement statement : ast.getStatements()) {
                ++indent;
                newline(indent);
                visit(statement);
                --indent;
            }
            newline(indent);
            writer.write("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        writer.write(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        // Type and type name
        writer.write(ast.getVariable().getType().getJvmName() + " " + ast.getName());

        // Has an assignment (this is optional)
        if (ast.getValue().isPresent()) {
            writer.write(" = ");
            visit(ast.getValue().get());
        }

        // Closing semicolon
        writer.write(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        writer.write(" = ");
        visit(ast.getValue());
        writer.write(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {

        // Opening condition
        writer.write("if (");
        visit(ast.getCondition());
        writer.write(") {");

        // If there's no if statements, write a closing brace on same line. Otherwise, write out the statements.
        if (ast.getThenStatements().isEmpty()) {
            writer.write("}");
        }
        else {
            for (Ast.Statement statement : ast.getThenStatements()) {
                ++indent;
                newline(indent);
                visit(statement);
                --indent;
            }
            newline(indent);
            writer.write("}");
        }

        // If there's else statements, write the else block containing all statements.
        if (!ast.getElseStatements().isEmpty()) {
            writer.write(" else {");
            for (Ast.Statement statement : ast.getElseStatements()) {
                ++indent;
                newline(indent);
                visit(statement);
                --indent;
            }
            newline(indent);
            writer.write("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        writer.write("for ( ");

        // Optional initialization, otherwise manually enter the semicolon only
        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
        }
        else {
            writer.write(";");
        }

        writer.write(" ");
        visit(ast.getCondition());
        writer.write("; ");

        // Optional increment
        if (ast.getIncrement() != null) { // TODO: Fix the fact that the statement generator is generating a semicolon at the end when you don't want it...
            visit(ast.getIncrement());
            writer.write(" ");
        }

        // Last space, closing parentheses, opening brace
        writer.write(") {");

        // Closing brace on same line if no statements present, otherwise print all statements and a closing brace on a new line
        if (ast.getStatements().isEmpty()) {
            writer.write("}");
        }
        else {
            for (Ast.Statement statement : ast.getStatements()) {
                ++indent;
                newline(indent);
                visit(statement);
                --indent;
            }
            newline(indent);
            writer.write("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {

        // Opening line
        writer.write("while (");
        visit(ast.getCondition());
        writer.write(") {");

        // If there's no statements, close braces on same line
        if (ast.getStatements().isEmpty()) {
            writer.write("}");
        }

        // Otherwise, write the statements and close on new line
        else {
            // Write out each statement
            for (Ast.Statement statement : ast.getStatements()) {
                ++indent;
                newline(indent);
                visit(statement);
                --indent;
            }

            // Close braces on new line
            newline(indent);
            writer.write("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        writer.write("return ");
        visit(ast.getValue());
        writer.write(";");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();

        // STRING or CHARACTER
        if (ast.getType().equals(Environment.Type.CHARACTER) || ast.getType().equals(Environment.Type.STRING)) {
            writer.write("\"" + literal.toString() + "\"");
        }
        // BOOLEAN
        else if (ast.getType().equals(Environment.Type.BOOLEAN)) {
            writer.write(literal.toString());
        }
        // INTEGER
        else if (ast.getType().equals(Environment.Type.INTEGER)) {
            writer.write(literal.toString());
        }
        // DECIMAL
        else if (ast.getType().equals(Environment.Type.DECIMAL)) {
            String numAsString = literal.toString();
            BigDecimal numAsBigDecimal = new BigDecimal(numAsString);
            writer.write(numAsBigDecimal.toString());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        writer.write("(");
        visit(ast.getExpression());
        writer.write(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        writer.write(" " + ast.getOperator() + " ");
        visit(ast.getRight());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        // TODO: Do you need to generate the receiver with a dot after it before writing the name or should this first part be removed?
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            writer.write(".");
        }

        writer.write(ast.getName());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        // TODO: Do you need to generate the receiver with a dot after it before writing the name or should this first part be removed?
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            writer.write(".");
        }

        writer.write(ast.getFunction().getJvmName() + "(");
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            if (i < ast.getArguments().size() - 1) {
                writer.write(", ");
            }
        }
        writer.write(")");

        return null;
    }
}
