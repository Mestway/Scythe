package lang.sql.ast.valnode;

import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.parameterized.InstantiateEnv;
import lang.sql.dataval.ValType;
import lang.sql.dataval.Value;
import lang.sql.ast.Environment;
import lang.sql.ast.Hole;
import lang.sql.ast.Node;
import lang.sql.exception.SQLEvalException;
import lang.sql.transformation.ValNodeSubstitution;

import java.util.List;

/**
 * Created by clwang on 12/16/15.
 */
public interface ValNode extends Node {
    Value eval(Environment env) throws SQLEvalException;
    String getName();
    ValType getType(EnumContext ctxt);
    String prettyPrint(int lv);

    /**
     * Determines the level of nested subqueries appear in this valnode
     * @return the level
     */
    int getNestedQueryLevel();
    boolean equalsToValNode(ValNode vn);

    List<Hole> getAllHoles();
    ValNode instantiate(InstantiateEnv env);
    ValNode subst(ValNodeSubstitution vnsb);
}
