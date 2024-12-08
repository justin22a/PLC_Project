package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }

        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }

        try {
            Environment.Function mainFunction = scope.lookupFunction("main", 0);
            return mainFunction.invoke(new ArrayList<>());
        } catch (RuntimeException e) {
            throw new RuntimeException("Main function with 0 arguments not found.");
        }
    }


    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        Environment.PlcObject value = ast.getValue()
                .map(this::visit)
                .orElse(Environment.NIL);

        scope.defineVariable(ast.getName(), ast.getConstant(), value);

        return Environment.NIL;
    }




    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope previousScope = scope; // Save the current scope
            try {
                scope = new Scope(scope);

                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), false, args.get(i));
                }

                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } catch (Return returnException) {
                return returnException.value;
            } finally {
                scope = previousScope; // Restore the original scope
            }
            return Environment.NIL;
        });

        return Environment.NIL;
    }





    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());

        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Environment.PlcObject value = ast.getValue()
                .map(this::visit)
                .orElse(Environment.NIL);

        scope.defineVariable(ast.getName(), false, value);

        return Environment.NIL;
    }



    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Invalid assignment target. Must be a variable or field.");
        }

        Ast.Expression.Access receiver = (Ast.Expression.Access) ast.getReceiver();
        Environment.PlcObject value = visit(ast.getValue());

        if (receiver.getReceiver().isPresent()) {
            Environment.PlcObject object = visit(receiver.getReceiver().get());
            object.setField(receiver.getName(), value);
        } else {
            Environment.Variable variable = scope.lookupVariable(receiver.getName());
            if (variable.getConstant()) {
                throw new RuntimeException("Cannot modify a constant variable: " + receiver.getName());
            }
            variable.setValue(value);
        }

        return Environment.NIL;
    }



    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        boolean condition = requireType(Boolean.class, visit(ast.getCondition()));

        try {
            scope = new Scope(scope);

            if (condition) {
                for (Ast.Statement stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } else {
                for (Ast.Statement stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            }
        } finally {
            scope = scope.getParent();
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
        }

        while (ast.getCondition() != null && requireType(Boolean.class, visit(ast.getCondition()))) {
            scope = new Scope(scope);
            try {
                for (Ast.Statement stmt : ast.getStatements()) {
                    visit(stmt);
                }
                if (ast.getIncrement() != null) {
                    visit(ast.getIncrement());
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }





    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        else {
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }


    // helper
    private static Comparable<Object> requireComparable(Environment.PlcObject object) {
        if (object.getValue() instanceof Comparable) {
            return (Comparable<Object>) object.getValue();
        } else {
            throw new RuntimeException("Non-comparable type: " + object.getValue().getClass().getSimpleName());
        }
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();

        switch (operator) {
            case "<":
                return Environment.create(requireComparable(visit(ast.getLeft())).compareTo(requireComparable(visit(ast.getRight()))) < 0);
            case "<=":
                return Environment.create(requireComparable(visit(ast.getLeft())).compareTo(requireComparable(visit(ast.getRight()))) <= 0);
            case ">":
                return Environment.create(requireComparable(visit(ast.getLeft())).compareTo(requireComparable(visit(ast.getRight()))) > 0);
            case ">=":
                return Environment.create(requireComparable(visit(ast.getLeft())).compareTo(requireComparable(visit(ast.getRight()))) >= 0);
            case "==":
                return Environment.create(visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue()));
            case "!=":
                return Environment.create(!visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue()));
            case "+":
                Environment.PlcObject lhs = visit(ast.getLeft());
                Environment.PlcObject rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof String || rhs.getValue() instanceof String) {
                    return Environment.create(lhs.getValue().toString() + rhs.getValue().toString());
                } else if (lhs.getValue() instanceof BigInteger && rhs.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) lhs.getValue()).add((BigInteger) rhs.getValue()));
                } else if (lhs.getValue() instanceof BigDecimal && rhs.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) lhs.getValue()).add((BigDecimal) rhs.getValue()));
                } else {
                    throw new RuntimeException("Invalid operands for + operator.");
                }
            case "-":
                lhs = visit(ast.getLeft());
                rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof BigInteger && rhs.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) lhs.getValue()).subtract((BigInteger) rhs.getValue()));
                } else if (lhs.getValue() instanceof BigDecimal && rhs.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) lhs.getValue()).subtract((BigDecimal) rhs.getValue()));
                } else {
                    throw new RuntimeException("Invalid operands for - operator.");
                }
            case "*":
                lhs = visit(ast.getLeft());
                rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof BigInteger && rhs.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) lhs.getValue()).multiply((BigInteger) rhs.getValue()));
                } else if (lhs.getValue() instanceof BigDecimal && rhs.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) lhs.getValue()).multiply((BigDecimal) rhs.getValue()));
                } else {
                    throw new RuntimeException("Invalid operands for * operator.");
                }
            case "&&":
                Environment.PlcObject lhsAnd = visit(ast.getLeft());
                if (!requireType(Boolean.class, lhsAnd)) {
                    return Environment.create(false);
                }
                return Environment.create(requireType(Boolean.class, visit(ast.getRight())));
            case "||":
                Environment.PlcObject lhsOr = visit(ast.getLeft());
                if (requireType(Boolean.class, lhsOr)) {
                    return Environment.create(true);
                }
                return Environment.create(requireType(Boolean.class, visit(ast.getRight())));
            case "/":
                lhs = visit(ast.getLeft());
                rhs = visit(ast.getRight());
                if (lhs.getValue() instanceof BigDecimal && rhs.getValue() instanceof BigDecimal) {
                    if (rhs.getValue().equals(BigDecimal.ZERO)) {
                        throw new RuntimeException("do nnot divide by zero.");
                    }
                    return Environment.create(
                            ((BigDecimal) lhs.getValue()).divide((BigDecimal) rhs.getValue(), RoundingMode.HALF_EVEN)
                    );
                } else if (lhs.getValue() instanceof BigInteger && rhs.getValue() instanceof BigInteger) {
                    if (rhs.getValue().equals(BigInteger.ZERO)) {
                        throw new RuntimeException("do nnot divide by zero.");
                    }
                    return Environment.create(((BigInteger) lhs.getValue()).divide((BigInteger) rhs.getValue()));
                } else {
                    throw new RuntimeException("Invalid operands for / operator.");
                }
            default:
                throw new RuntimeException("Unknown binary operator: " + operator);
        }
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject object = visit(ast.getReceiver().get());
            return object.getField(ast.getName()).getValue();  // Retrieve the field value
        } else {
            try {
                return scope.lookupVariable(ast.getName()).getValue();  // Retrieve the variable value
            } catch (RuntimeException e) {
                System.out.println("What the fuckkkkkkk");
                throw new RuntimeException("Variable '" + ast.getName() + "' is not defined in the current scope.");
            }
        }
    }



    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> arguments = new ArrayList<>();
        for (Ast.Expression argument : ast.getArguments()) {
            arguments.add(visit(argument)); //grab each one
        }

        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.callMethod(ast.getName(), arguments);
        } else {
            return scope.lookupFunction(ast.getName(), arguments.size()).invoke(arguments);
        }
    }

    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
