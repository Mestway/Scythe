package lang.sql.ast.valnode;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.sql.dataval.Value;
import lang.sql.dataval.ValType;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;
import util.IndentationManager;

import java.util.*;

/**
 * Created by clwang on 1/8/16.
 */
public class ValHole implements ValNode, Hole {

    int id = 0; // same id can have several versions with different types
    ValType type = null;

    private static int idCounter = 0;

    public static List<ValNode> genHoles(int number) {
        return genHolesWithType(number, Arrays.asList(ValType.values()));
    }

    public static List<ValNode> genHolesWithType(int number, List<ValType> types) {
        List<ValNode> holes = new ArrayList<>();
        for (int i = 0; i < number; i ++) {
            for (ValType vt : types) {
                holes.add(ValHole.assignHole(i, vt));
            }
        }
        return holes;
    }

    private static ValHole assignHole(int id, ValType vt) {
        ValHole vh = new ValHole();
        vh.id = id;
        //vh.uniqueID = idCounter ++;
        vh.type = vt;
        return vh;
    }

    private ValHole() {};

    public int getId() { return this.id; }

    @Override
    public Value eval(Environment env) throws SQLEvalException {
        throw new SQLEvalException("Evaluation on a hole value");
    }

    @Override
    public String getName() {
        return "Hole";
    }

    @Override
    public ValType getType(EnumContext ctxt) {
        return this.type;
    }

    @Override
    public String prettyPrint(int lv) {
        return IndentationManager.addIndention("ValHole#" + this.id + ":" + this.type.toString(), lv);
    }

    @Override
    public int getNestedQueryLevel() {
        return 0;
    }

    @Override
    public boolean equalsToValNode(ValNode vn) {
        return false;
    }

    public static boolean containsSameHoleDiffType(List<ValHole> vhs) {
        for (int i = 0; i < vhs.size(); i ++) {
            for (int j = i + 1; j < vhs.size(); j ++) {
                if (vhs.get(i).id == vhs.get(j).id && !(vhs.get(i).type.equals(vhs.get(j).type))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Hole> getAllHoles() {
        return Arrays.asList(this);
    }

    @Override
    public ValNode instantiate(InstantiateEnv env) {
        return env.substValHole(this);
    }

    @Override
    public ValNode subst(ValNodeSubstitution vnb) {
        return this;
    }
}
