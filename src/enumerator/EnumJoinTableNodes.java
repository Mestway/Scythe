package enumerator;

import sql.lang.ast.table.JoinNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by clwang on 1/7/16.
 */
public class EnumJoinTableNodes {

    /*****************************************************
     Enumeration by join
     1. Enumerate atomic tables and then do join
     *****************************************************/

    public static List<TableNode> enumJoinNode(EnumContext ec, int depth) {

        List<TableNode> basicTables = TableEnumerator.enumTable(ec, depth - 1);

        List<TableNode> joinTables = new ArrayList<>();
        int sz = basicTables.size();
        for (int i = 0; i < sz; i ++) {
            for (int j = 0; j < sz; j ++) {
                // TODO: think carefully
                if (i == j)
                    continue;
                if (! (basicTables.get(j) instanceof NamedTable))
                    continue;
                TableNode jn = new JoinNode(
                        Arrays.asList(
                                basicTables.get(i),
                                basicTables.get(j)
                        )
                );
                joinTables.add(jn);
            }
        }
        return joinTables;
    }
}
