package enumerator;

import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.TableAsVal;
import sql.lang.ast.val.ValNode;

import java.util.ArrayList;
import java.util.List;

/**
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
