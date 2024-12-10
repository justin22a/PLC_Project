

package plc.project;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

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
            writer.write(" ");
        }
    }
    private String changeName(String name){
        if (Objects.equals(name, "Integer")){
            return "int";
        }else if (Objects.equals(name, "Boolean")){
            return "boolean";
        }else if (Objects.equals(name, "Decimal")){
            return "double";
        }
        return name;
    }
    @Override
    public Void visit(Ast.Source ast) {
        writer.write("public class Main {");
        indent += 4;
        if (!ast.getFields().isEmpty()) {
            newline(0);
            for (Ast.Field field : ast.getFields()) {
                newline(indent);
                writer.write(field.toString());
            }
        }
        if (!ast.getMethods().isEmpty()) {
            newline(0);
        }
        newline(indent);
        writer.write("public static void main(String[] args) {");
        newline(indent += 4);

        writer.write("System.exit(new Main().main());");
        newline(indent -= 4);
        writer.write("}");
        if (!ast.getMethods().isEmpty()) {
            for (Ast.Method method : ast.getMethods()) {
                newline(0);
                newline(indent);

                print(method);
            }
        }
        indent -= 2;
        indent -= 2;
        newline(0);
        newline(indent);

        writer.write("}");

        return null;
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
        String mType = changeName(ast.getFunction().getType().getJvmName());
        writer.write(mType);
        writer.write(" ");
        writer.write(ast.getName());
        writer.write("(");
        if (!ast.getParameters().isEmpty()) {
            for (int i = 0; i < ast.getParameters().size(); i++) {
                if (i > 0) {
                    writer.write(", ");
                }
                String paramType = changeName(ast.getParameterTypeNames().get(i));
                writer.write(paramType);
                writer.write(" ");
                writer.write(ast.getParameters().get(i));
            }
        }
        writer.write(") {");
        if (ast.getStatements().isEmpty()) {
            writer.write("}");
        }
        else
        {
            for (Ast.Statement statement : ast.getStatements()) {
                newline(indent += 4);
                print(statement);
                indent -= 4;
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
    public Void visit(Ast.Statement.If ast){
        boolean temp = ast.getThenStatements().isEmpty();

        if (temp){
            writer.write("if (");
            writer.write(ast.getCondition().toString());

            writer.write(") {}");
        }else{
            writer.write("if (");

            print(ast.getCondition());
            writer.write(") {");

            for (int i = 0; i < ast.getThenStatements().size(); i++){
                newline(indent+=4);

                print(ast.getThenStatements().get(i));
                indent-=4;
            }
            temp = !ast.getElseStatements().isEmpty();
            if (temp){
                newline(0);
                writer.write("} else {");

                for (int i = 0; i < ast.getElseStatements().size(); i++){
                    newline(indent+=4);
                    print(ast.getElseStatements().get(i));
                    indent-=4;
                }
            }
            newline(indent);
            writer.write("}");
        }
        return null;
    }


    @Override
    public Void visit(Ast.Statement.For ast){
        writer.write("for ( ");

        if (ast.getInitialization() != null) {
            print(ast.getInitialization());
        }else{

            writer.write(";");
        }
        writer.write(" ");
        print(ast.getCondition());
        writer.write(";");

        if (ast.getIncrement() != null){
            generatingIncrement = true;
            writer.write(" ");
            print(ast.getIncrement());
            generatingIncrement = false;
        }
        writer.write(" ) ");
        boolean temp = ast.getStatements().isEmpty();
        if (temp) {
            writer.write("{}");

        } else {
            writer.write("{");
            for (Ast.Statement statement : ast.getStatements()) {
                newline(indent+=4);

                print(statement);

                indent-=4;
            }
            newline(indent);
            writer.write("}");
        }
        return null;}

    @Override
    public Void visit(Ast.Statement.While ast) {
        writer.write("while (");
        visit(ast.getCondition());
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
    public Void visit(Ast.Statement.Return ast) {
        writer.write("return ");
        visit(ast.getValue());
        writer.write(";");
        return null;
    }
    @Override
    public Void visit(Ast.Expression.Literal ast){
        boolean tempc = ast.getType() == Environment.Type.CHARACTER;
        if(tempc){
            writer.write("'");
            writer.write(ast.getLiteral().toString());
            writer.write("'");
        }
        else if (ast.getType() == Environment.Type.STRING){
            writer.write("\"");

            writer.write(ast.getLiteral().toString());
            writer.write("\"");
        }
        else if (ast.getType() == Environment.Type.DECIMAL){

            print(((BigDecimal) ast.getLiteral()).toString());

        }
        else if (ast.getType() == Environment.Type.INTEGER){
            print(((BigInteger) ast.getLiteral()).toString());

        }else
        {
            writer.write(ast.getLiteral().toString());
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