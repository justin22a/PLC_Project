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

        //  Fields
        for (Ast.Field field : ast.getFields()) {
            newline(1);
            visit(field);
        }

        // Pub stat void main stuff
        newline(0);
        newline(1);
        writer.write("public static void main(String[] args) {");
        newline(2);
        writer.write("System.exit(new Main().main());");
        newline(1);
        writer.write("}");

        // Methods
        for (Ast.Method method : ast.getMethods()) {
            newline(1);
            visit(method);
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
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

        // No statements? -> close braces on same line
        if (ast.getStatements().isEmpty()) {
            writer.write("}");
        }

        // Otherwise, write the statements and close on new line
        else {
            // Write out each statement
            for (Ast.Statement statement : ast.getStatements()) {
                newline(1);
                visit(statement);
            }

            // Close braces on new line
            newline(0);
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
