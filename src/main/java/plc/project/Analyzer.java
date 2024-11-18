package plc.project;
// ayo
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }

        boolean hasMainMethod = false;
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
            if (method.getName().equals("main") && method.getParameters().isEmpty()) {
                if (method.getReturnTypeName().isPresent() && method.getReturnTypeName().get().equals("Integer")) {
                    hasMainMethod = true;
                }
            }
        }

        if (!hasMainMethod) {
            throw new RuntimeException("A main/0 function with an Integer return type is required.");
        }

        return null;
    }


    @Override
    public Void visit(Ast.Field ast) {
        Environment.Type fieldType;
        try {
            fieldType = Environment.getType(ast.getTypeName());
        } catch (RuntimeException e) {
            throw new RuntimeException("Unknown type: " + ast.getTypeName());
        }

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            Environment.Type valueType = ast.getValue().get().getType();

            requireAssignable(fieldType, valueType);
        } else if (ast.getConstant()) {
            throw new RuntimeException("Constant field must have an initial value.");
        }

        Environment.Variable variable = scope.defineVariable(ast.getName(), ast.getName(), fieldType, ast.getConstant(), Environment.NIL);
        ast.setVariable(variable);

        return null;
    }


    @Override
    public Void visit(Ast.Method ast) {
        Environment.Type returnType = ast.getReturnTypeName().isPresent()
                ? Environment.getType(ast.getReturnTypeName().get())
                : Environment.Type.NIL;

        List<Environment.Type> parameterTypes = ast.getParameterTypeNames().stream()
                .map(typeName -> {
                    try {
                        return Environment.getType(typeName);
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Unknown type: " + typeName);
                    }
                })
                .collect(Collectors.toList());

        Environment.Function function = scope.defineFunction(
                ast.getName(),
                ast.getName(),
                parameterTypes,
                returnType,
                args -> Environment.NIL
        );
        ast.setFunction(function);

        Scope methodScope = new Scope(scope);
        for (int i = 0; i < ast.getParameters().size(); i++) {
            String paramName = ast.getParameters().get(i);
            Environment.Type paramType = parameterTypes.get(i);
            methodScope.defineVariable(paramName, paramName, paramType, false, Environment.NIL);
        }

        Scope previousScope = scope;
        scope = methodScope;
        this.method = ast;

        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }

        scope = previousScope;
        this.method = null;

        return null;
    }


    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());

        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expression statement must be a function call.");
        }

        return null;
    }


    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type declaredType = null;

        if (ast.getTypeName().isPresent()) {
            try {
                declaredType = Environment.getType(ast.getTypeName().get());
            } catch (RuntimeException e) {
                throw new RuntimeException("Unknown type: " + ast.getTypeName().get());
            }
        }

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            Environment.Type valueType = ast.getValue().get().getType();

            if (declaredType != null) {
                requireAssignable(declaredType, valueType);
            } else {
                declaredType = valueType;
            }
        } else if (declaredType == null) {
            throw new RuntimeException("Declaration must have a type or an initial value.");
        }

        Environment.Variable variable = scope.defineVariable(ast.getName(), ast.getName(), declaredType, false, Environment.NIL);
        ast.setVariable(variable);

        return null;
    }


    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());

        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Assignment receiver must be an access exprssion.");
        }

        visit(ast.getValue());

        Environment.Type receiverType = ast.getReceiver().getType();
        Environment.Type valueType = ast.getValue().getType();

        requireAssignable(receiverType, valueType);

        Ast.Expression.Access access = (Ast.Expression.Access) ast.getReceiver();
        if (access.getVariable().getConstant()) {
            throw new RuntimeException("Canot sign to a constant variable.");
        }

        return null;
    }


    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Condition must be of type Boolean.");
        }
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Then block must not be empty.");
        }

        Scope thenScope = new Scope(scope);
        Scope previousScope = scope;
        scope = thenScope;
        for (Ast.Statement statement : ast.getThenStatements()) {
            visit(statement);
        }
        scope = previousScope;

        if (!ast.getElseStatements().isEmpty()) {
            Scope elseScope = new Scope(scope);
            scope = elseScope;
            for (Ast.Statement statement : ast.getElseStatements()) {
                visit(statement);
            }
            scope = previousScope;
        }

        return null;
    }


    @Override
    public Void visit(Ast.Statement.For ast) {
        if (ast.getCondition() != null) {
            visit(ast.getCondition());
            if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
                throw new RuntimeException("For loop condition must be of type Boolean.");
            }
        }

        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
        }

        if (ast.getIncrement() != null) {
            visit(ast.getIncrement());
        }

        if (ast.getStatements().isEmpty()) {
            throw new RuntimeException("For loop body must not be empty.");
        }

        Scope loopScope = new Scope(scope);
        Scope previousScope = scope;
        scope = loopScope;
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        scope = previousScope;

        return null;
    }


    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("While loop condition must be of type Boolean.");
        }

        if (ast.getStatements().isEmpty()) {
            throw new RuntimeException("While loop body must not be empty.");
        }

        Scope loopScope = new Scope(scope);
        Scope previousScope = scope;
        scope = loopScope;
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        scope = previousScope;

        return null;
    }


    @Override
    public Void visit(Ast.Statement.Return ast) {
        if (method == null) {
            throw new RuntimeException("Return statement must be inside a method.");
        }

        visit(ast.getValue());
        Environment.Type returnType = ast.getValue().getType();

        Environment.Type expectedReturnType = method.getReturnTypeName().isPresent()
                ? Environment.getType(method.getReturnTypeName().get())
                : Environment.Type.NIL;

        requireAssignable(expectedReturnType, returnType);

        return null;
    }


    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literalValue = ast.getLiteral();

        if (literalValue == null) {
            ast.setType(Environment.Type.NIL);
        } else if (literalValue instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (literalValue instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (literalValue instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (literalValue instanceof BigInteger) {
            BigInteger intValue = (BigInteger) literalValue;
            if (intValue.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0 && intValue.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0) {
                ast.setType(Environment.Type.INTEGER);
            } else {
                throw new RuntimeException("Integer literal out of range.");
            }
        } else if (literalValue instanceof BigDecimal) {
            BigDecimal decimalValue = (BigDecimal) literalValue;
            try {
                decimalValue.doubleValue();
                ast.setType(Environment.Type.DECIMAL);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Decimal literal out of range.");
            }
        } else {
            throw new RuntimeException("Unsupported literal type: " + literalValue.getClass().getSimpleName());
        }

        return null;
    }


    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());

        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Group expression must contain a binary expression.");
        }

        ast.setType(ast.getExpression().getType());

        return null;
    }


    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());

        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();

        switch (ast.getOperator()) {
            case "&&":
            case "||":
                if (!leftType.equals(Environment.Type.BOOLEAN) || !rightType.equals(Environment.Type.BOOLEAN)) {
                    throw new RuntimeException("Logical operators require both operands to be Boolean.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;

            case "<":
            case "<=":
            case ">":
            case ">=":
            case "==":
            case "!=":
                if (!leftType.equals(rightType) || !Environment.Type.COMPARABLE.getScope().lookupVariable(leftType.getName()).getType().equals(leftType)) {
                    throw new RuntimeException("Comparison operators require both operands to be Comparable and of the same type.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;

            case "+":
                if (leftType.equals(Environment.Type.STRING) || rightType.equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                } else if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else {
                    throw new RuntimeException("Invalid types for addition.");
                }
                break;

            case "-":
            case "*":
            case "/":
                if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else {
                    throw new RuntimeException("Invalid types for arithmetic operation.");
                }
                break;

            default:
                throw new RuntimeException("Unsupported operator: " + ast.getOperator());
        }

        return null;
    }


    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            Environment.Type receiverType = ast.getReceiver().get().getType();
            Environment.Variable variable = receiverType.getField(ast.getName());
            ast.setVariable(variable);
        } else {
            Environment.Variable variable = scope.lookupVariable(ast.getName());
            ast.setVariable(variable);
        }

        return null;
    }


    @Override
    public Void visit(Ast.Expression.Function ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            Environment.Type receiverType = ast.getReceiver().get().getType();
            Environment.Function function = receiverType.getFunction(ast.getName(), ast.getArguments().size());
            ast.setFunction(function);
        } else {
            Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            ast.setFunction(function);
        }

        List<Environment.Type> parameterTypes = ast.getFunction().getParameterTypes();
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            requireAssignable(parameterTypes.get(i), ast.getArguments().get(i).getType());
        }

        return null;
    }


    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(type) || target.equals(Environment.Type.ANY)) {
            return;
        }
        if (target.equals(Environment.Type.COMPARABLE) && (
                type.equals(Environment.Type.INTEGER) ||
                        type.equals(Environment.Type.DECIMAL) ||
                        type.equals(Environment.Type.CHARACTER) ||
                        type.equals(Environment.Type.STRING))) {
            return;
        }
        throw new RuntimeException("Type " + type.getName() + " is not assignable to " + target.getName());
    }


}
