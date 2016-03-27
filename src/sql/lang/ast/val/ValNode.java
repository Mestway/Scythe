package sql.lang.ast.val;

import enumerator.context.EnumContext;
import enumerator.parameterized.InstantiateEnv;
import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.ast.Node;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;

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
    ValNode subst(ValNodeSubstBinding vnsb);
}
