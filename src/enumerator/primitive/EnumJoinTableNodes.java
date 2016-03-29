package enumerator.primitive;

import enumerator.context.EnumContext;
import sql.lang.DataType.ValType;
import sql.lang.Table;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.JoinNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import util.RenameTNWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enumerate join table nodes with given EC
 * Created by clwang on 1/7/16.
 */
public class EnumJoinTableNodes {

    /*****************************************************
     Enumeration by join
     1. Enumerate atomic tables and then do join
     *****************************************************/

    // This is a simpler version of joining considering no filters at this stage,
    // Joining is only a matter of performing cartesian production here.
    public static List<JoinNode> enumJoinWithoutFilter(EnumContext ec) {
        List<TableNode> basicTables =  ec.getTableNodes();
        List<JoinNode> joinTables = new ArrayList<>();
        int sz = basicTables.size();
        for (int i = 0; i < sz; i ++) {
            for (int j = 0; j < sz; j++) {
                if (i == j)
                    continue;
                if (! (basicTables.get(j) instanceof NamedTable))
                  continue;
                JoinNode jn = new JoinNode(
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

    // This is the join we used in canonicalSQL,
    // filters are used in enumerating canonical join nodes.
    public static List<TableNode> enumJoinWithJoin(EnumContext ec) {

        List<TableNode> basicTables =  ec.getTableNodes();

        List<TableNode> joinTables = new ArrayList<>();
        int sz = basicTables.size();
        for (int i = 0; i < sz; i ++) {
            for (int j = 0; j < sz; j ++) {
                // TODO: think carefully
                if (i == j)
                    continue;

                boolean isPrimitiveTable = false;
                if (! (basicTables.get(j) instanceof NamedTable))
                    continue;
                else {
                    Table bj = ((NamedTable) basicTables.get(j)).getTable();
                    for (Table t : ec.getInputs())
                        if (bj.containsContent(t)) {
                            isPrimitiveTable = true;
                        }
                }
                isPrimitiveTable = true;

                if (!isPrimitiveTable)
                    continue;

                TableNode jn = new JoinNode(
                        Arrays.asList(
                                basicTables.get(i),
                                basicTables.get(j)
                        )
                );

                joinTables.add(jn);

                // enumPossibleFilters for the node
                int lSchemaSize = basicTables.get(i).getSchema().size();
                int rSchemaSize = basicTables.get(j).getSchema().size();
                TableNode renamedJN = RenameTNWrapper.tryRename(jn);

                // Create the L and R set for filter enumeration
                List<ValNode> L = new ArrayList<>();
                List<ValNode> R = new ArrayList<>();
                for (int k = 0; k < lSchemaSize; k ++) {
                    L.add(new NamedVal(renamedJN.getSchema().get(k)));
                }
                for (int k = 0; k < rSchemaSize; k ++) {
                    R.add(new NamedVal(renamedJN.getSchema().get(lSchemaSize + k)));
                }

                Map<String, ValType> schemaTypeMap = new HashMap<>();
                for (int k = 0; k < renamedJN.getSchema().size(); k ++) {
                    schemaTypeMap.put(renamedJN.getSchema().get(k), renamedJN.getSchemaType().get(k));
                }
                EnumContext ec2 = ec.extendTypeMap(ec, schemaTypeMap);

                List<Filter> filters = FilterEnumerator.enumFiltersLR(L, R, ec2);

                for (Filter f : filters) {
                    TableNode tn = new SelectNode(
                            renamedJN.getSchema().stream().map(s -> new NamedVal(s)).collect(Collectors.toList()),
                            renamedJN,
                            f);
                    joinTables.add(tn);
                }
            }
        }

        return joinTables;
    }
}
