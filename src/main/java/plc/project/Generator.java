package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;
    private boolean generatingIncrement = false;

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

    private String mapTypeName(String typeName) {
        switch (typeName) {
            case "Integer": return "int";
            case "Decimal": return "double";
            case "String": return "String";
            case "Boolean": return "boolean";
            default: return typeName;
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        writer.write("public class Main {");
        newline(0);
        indent++;

        // Fields
        for (Ast.Field field : ast.getFields()) {
            newline(indent);
            visit(field);
        }

        if (!ast.getFields().isEmpty() && !ast.getMethods().isEmpty()) {
            newline(0);
        }

        // Main method
        newline(indent);
        writer.write("public static void main(String[] args) {");
        indent++;
        newline(indent);
        writer.write("System.exit(new Main().main());");
        indent--;
        newline(indent);
        writer.write("}");

        // Methods
        for (Ast.Method method : ast.getMethods()) {
            newline(0);
            newline(indent);
            visit(method);
        }

        indent--;
        newline(0);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getConstant()) {
            writer.write("final ");
        }
        writer.write(mapTypeName(ast.getTypeName()) + " " + ast.getName());
        if (ast.getValue().isPresent()) {
            writer.write(" = ");
            visit(ast.getValue().get());
        }
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        writer.write(mapTypeName(ast.getFunction().getReturnType().getJvmName()) + " " + ast.getName() + "(");
        for (int i = 0; i < ast.getParameters().size(); i++) {
            writer.write(mapTypeName(ast.getFunction().getParameterTypes().get(i).getJvmName()) + " " + ast.getParameters().get(i));
            if (i < ast.getParameters().size() - 1) {
                writer.write(", ");
            }
        }
        writer.write(") {");
        if (ast.getStatements().isEmpty()) {
            writer.write("}");
        } else {
            for (Ast.Statement statement : ast.getStatements()) {
                indent++;
                newline(indent);
                visit(statement);
                indent--;
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
        writer.write(mapTypeName(ast.getVariable().getType().getJvmName()) + " " + ast.getName());
        if (ast.getValue().isPresent()) {
            writer.write(" = ");
            visit(ast.getValue().get());
        }
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        writer.write(" = ");
        visit(ast.getValue());
        if (!generatingIncrement) {
            writer.write(";");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        writer.write("if (");
        visit(ast.getCondition());
        writer.write(") {");
        for (Ast.Statement statement : ast.getThenStatements()) {
            indent++;
            newline(indent);
            visit(statement);
            indent--;
        }
        newline(indent);
        writer.write("}");
        if (!ast.getElseStatements().isEmpty()) {
            writer.write(" else {");
            for (Ast.Statement statement : ast.getElseStatements()) {
                indent++;
                newline(indent);
                visit(statement);
                indent--;
            }
            newline(indent);
            writer.write("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        writer.write("for (");
        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
        } else {
            writer.write(";");
        }
        writer.write(" ");
        visit(ast.getCondition());
        writer.write("; ");
        if (ast.getIncrement() != null) {
            generatingIncrement = true;
            visit(ast.getIncrement());
            generatingIncrement = false;
        }
        writer.write(") {");
        for (Ast.Statement statement : ast.getStatements()) {
            indent++;
            newline(indent);
            visit(statement);
            indent--;
        }
        newline(indent);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        writer.write("while (");
        visit(ast.getCondition());
        writer.write(") {");
        for (Ast.Statement statement : ast.getStatements()) {
            indent++;
            newline(indent);
            visit(statement);
            indent--;
        }
        newline(indent);
        writer.write("}");
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
        if (literal == null) {
            writer.write("null");
        } else if (literal instanceof Character) {
            writer.write("'" + literal.toString().replace("'", "\\'") + "'");
        } else if (literal instanceof String) {
            writer.write("\"" + literal.toString().replace("\"", "\\\"") + "\"");
        } else if (literal instanceof BigDecimal) {
            writer.write(((BigDecimal) literal).toPlainString());
        } else {
            writer.write(literal.toString());
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
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            writer.write(".");
        }
        writer.write(ast.getVariable().getJvmName());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
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
