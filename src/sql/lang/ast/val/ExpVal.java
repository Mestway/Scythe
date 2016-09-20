package sql.lang.ast.val;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import sql.lang.datatype.NumberVal;
import sql.lang.datatype.ValType;
import sql.lang.datatype.Value;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Performing operations on values:
 *  ValNode op ValNode -> Val
 * Created by clwang on 12/17/15.
 */
public class ExpVal implements ValNode {

    BiFunction<Value, Value, Value> operator;
    List<ValNode> operands;
    String name = "";

    public ExpVal(BiFunction<Value,Value,Value> op, List<ValNode> operands) {
        this.operator = op;
        this.operands = operands;
    }

    public ExpVal(BiFunction<Value,Value,Value> op, List<ValNode> operands, String name) {
        this.operator = op;
        this.operands = operands;
        this.name = name;
    }

    @Override
    public Value eval(Environment env) throws SQLEvalException {
        if (operator.equals(plus) || operator.equals(minus) || operator.equals(times)) {
            if (operands.size() < 2) {
                System.err.println("[Error@ExpVal54] The number of operands is not enough.");
            }
            return operator.apply(operands.get(0).eval(env), operands.get(1).eval(env));
        };

        System.err.println("[Error@ExpVal59] More functions are not supported yet.");
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    // The type of the expression val is the value of its first argument
    public ValType getType(EnumContext ctxt) {
        return operands.get(0).getType(ctxt);
    }

    @Override
    public String prettyPrint(int lv) {
        return IndentionManagement.addIndention("(" + operands.get(0) + FuncName(this.operator) + operands.get(1) + ")", lv);
    }

    @Override
    public int getNestedQueryLevel() {
        int lv = 0;
        for (ValNode vn : this.operands)
            if (vn.getNestedQueryLevel() > lv)
                lv = vn.getNestedQueryLevel();
        return lv;
    }

    @Override
    public boolean equalsToValNode(ValNode vn) {
        if (vn instanceof ExpVal) {
            boolean eq = true;
            if (!this.operator.equals(((ExpVal) vn).operator))
                return false;
            if (this.operands.size() != ((ExpVal) vn).operands.size())
                return false;
            for (int i = 0; i < this.operands.size(); i ++) {
                if (! this.operands.get(i).equalsToValNode(((ExpVal) vn).operands.get(i)))
                    return false;
            }
            // ## did not care about the name, probably should fix it
            return true;
        }

        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        List<Hole> result = new ArrayList<>();
        this.operands.forEach(vn -> result.addAll(vn.getAllHoles()));
        return result;
    }

    @Override
    public ValNode instantiate(InstantiateEnv env) {
        return new ExpVal(
                this.operator,
                this.operands.stream().map(o -> o.instantiate(env)).collect(Collectors.toList()),
                this.name);
    }

    @Override
    public ValNode subst(ValNodeSubstBinding vnsb) {
        return new ExpVal(
                this.operator,
                this.operands.stream().map(o -> o.subst(vnsb)).collect(Collectors.toList()),
                this.name
        );
    }

    // preset functions
    public static BiFunction<Value, Value, Value> plus = (v1, v2) -> {
        if (v1 instanceof NumberVal && v2 instanceof NumberVal) {
            return new NumberVal(((NumberVal) v1).getVal() + ((NumberVal)v2).getVal());
        }
        System.err.println("[Error@ExpVal18] The function cannot be applied to two values of wrong types: " + v1 + " " + v2);
        return null;
    };

    public static BiFunction<Value, Value, Value> minus = (v1, v2) -> {
        if (v1 instanceof NumberVal && v2 instanceof NumberVal) {
            return new NumberVal(((NumberVal) v1).getVal() - ((NumberVal)v2).getVal());
        }
        System.err.println("[Error@ExpVal18] The function cannot be applied to two values of wrong types: " + v1 + " " + v2);
        return null;
    };

    public static BiFunction<Value, Value, Value> times = (v1, v2) -> {
        if (v1 instanceof NumberVal && v2 instanceof NumberVal) {
            return new NumberVal(((NumberVal) v1).getVal() * ((NumberVal)v2).getVal());
        }
        System.err.println("[Error@ExpVal18] The function cannot be applied to two values of wrong types: " + v1 + " " + v2);
        return null;
    };

    private static String FuncName(BiFunction<Value, Value, Value> func) {
        if (func.equals(plus))
            return "+";
        else if (func.equals(minus))
            return "-";
        else if (func.equals(times))
            return "*";
        else
            return "op";
    }
}