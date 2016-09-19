package forward_enumeration.primitive;

import forward_enumeration.context.EnumContext;
import sql.lang.ast.val.ValNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The primitive enumerator for value enumeration:
 *  Given an enumeration context, enumerate what values that can be obtained from EC
 * Created by clwang on 1/7/16.
 */
public class ValueEnumerator {

    public static List<ValNode> enumValNodes(EnumContext ec) {

        List<ValNode> result = new ArrayList<>();
        result.addAll(ec.getValNodes());

        /*
        List<TableNode> tns = new TableEnumerator().enumTable(ec, maxDepth);

        for (TableNode tn : tns) {
            result.add(new TableAsVal(tn, "v"));
        }*/

        return result;
    }
}
