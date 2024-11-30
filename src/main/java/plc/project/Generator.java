package plc.project;

import java.io.PrintWriter;

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
        for (Ast.Field field : ast.getFields()) {
            newline(indent);
            visit(field);
        }

        // Pub stat void main stuff
        newline(0);
        newline(indent);
        writer.write("public static void main(String[] args) {");
        newline(indent+1);
        writer.write("System.exit(new Main().main());");
        newline(indent);
        writer.write("}");

        // Methods
        for (Ast.Method method : ast.getMethods()) {
            newline(indent);
            visit(method);
            newline(0);
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
        throw new UnsupportedOperationException(); //TODO
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
        writer.write(ast.getTypeName() + " " + ast.getName()); // TODO: Gonna be a problem that typeName is optional in the Ast class?

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
                newline(indent+1);
                visit(statement);
            }
            newline(indent);
            writer.write("}");
        }

        // If there's else statements, write the else block containing all statements.
        if (!ast.getElseStatements().isEmpty()) {
            writer.write(" else {");
            for (Ast.Statement statement : ast.getElseStatements()) {
                newline(indent+1);
                visit(statement);
            }
            newline(indent);
            writer.write("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException(); //TODO
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
                newline(indent+1);
                visit(statement);
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }
}
