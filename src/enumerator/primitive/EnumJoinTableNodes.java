package enumerator.primitive;

import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import sql.lang.DataType.ValType;
import sql.lang.Table;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.*;
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


    //public static void emitEnumJoinWithoutFilterN(int tablenum, EnumContext ec, QueryChest qc)

    /*****************************************************
     Enumeration by join
     1. Enumerate atomic tables and then do join
     *****************************************************/

    public static void emitEnumJoinWithoutFilter(EnumContext ec, QueryChest qc) {
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

                qc.updateQuery(jn);
            }
        }
    }

    public static void emitEnumJoinWithFilter(EnumContext ec, QueryChest qc) {
        List<TableNode> basicTables =  ec.getTableNodes();
        int sz = basicTables.size();
        for (int i = 0; i < sz; i ++) {
            for (int j = 0; j < sz; j++) {

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

                if (!isPrimitiveTable)
                    continue;

                if (! (basicTables.get(j) instanceof NamedTable))
                    continue;
                JoinNode jn = new JoinNode(
                        Arrays.asList(
                                basicTables.get(i),
                                basicTables.get(j)
                        )
                );
                RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);

                // add the query without join
                qc.updateQuery(rt);

                List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterJoinNode(rt, ec);
                for (Filter f : filters) {
                    // the selection args are complete
                    List<ValNode> vals = rt.getSchema().stream()
                            .map(s -> new NamedVal(s))
                            .collect(Collectors.toList());
                    qc.updateQuery(RenameTNWrapper.tryRename(new SelectNode(vals, rt, f)));
                }
            }
        }
    }

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
    public static List<TableNode> enumJoinWithFilter(EnumContext ec) {

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

                if (!isPrimitiveTable)
                    continue;

                TableNode jn = new JoinNode(
                        Arrays.asList(
                                basicTables.get(i),
                                basicTables.get(j)
                        )
                );
                // add the original join table node without filter
                joinTables.add(jn);

                // be wary
                RenameTableNode renamedJN = (RenameTableNode)  RenameTNWrapper.tryRename(jn);
                // ask the canonical filter enumerator to enumerate the filter for this
                List<Filter> filters = EnumCanonicalFilters.enumCanonicalFilterJoinNode(renamedJN, ec);
                for (Filter f : filters) {
                    TableNode tn = new SelectNode(
                            renamedJN.getSchema().stream().map(s -> new NamedVal(s)).collect(Collectors.toList()),
                            renamedJN,
                            f);
                    // add join nodes with filters
                    joinTables.add(tn);
                }
            }
        }

        return joinTables;
    }
}
