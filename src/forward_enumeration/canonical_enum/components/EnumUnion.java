package forward_enumeration.canonical_enum.components;

import forward_enumeration.container.QueryContainer;
import forward_enumeration.context.EnumContext;
import forward_enumeration.primitive.FilterEnumerator;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by clwang on 10/22/16.
 */
public class EnumUnion {

    public static List<TableNode> enumUnion(List<TableNode> leftSet, List<TableNode> rightSet) {

        List<TableNode> result = new ArrayList<>();

        for (TableNode ti : leftSet) {
            for (TableNode tj : rightSet) {

                List<TableNode> tns = Arrays.asList(ti, tj);

                UnionNode jn = new UnionNode(tns);

                Table t1 = null, t2 = null;

                try {
                    t1 = tns.get(0).eval(new Environment());
                    t2 = tns.get(1).eval(new Environment());
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }

                if (! Table.schemaMatch(t1, t2))
                    continue;

                RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);
                result.add(rt);
            }
        }
        return result;
    }

    public static List<Table> emitEnumerateUnion(List<TableNode> leftSet,
                                          List<TableNode> RightSet,
                                          QueryContainer qc) {

        List<Table> newlyGeneratedTables = new ArrayList<>();

        for (TableNode ti : leftSet) {
            for (TableNode tj : RightSet) {

                List<TableNode> tns = Arrays.asList(ti, tj);

                UnionNode jn = new UnionNode(tns);

                Table t1 = null, t2 = null;

                try {
                    t1 = qc.getRepresentative(tns.get(0).eval(new Environment()));
                    t2 = qc.getRepresentative(tns.get(1).eval(new Environment()));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }

                if (! Table.schemaMatch(t1, t2))
                    continue;


                RenameTableNode rt = (RenameTableNode) RenameTNWrapper.tryRename(jn);
                // add the query without join
                qc.insertQuery(rt);

                try {
                    Table rtt = rt.eval(new Environment());
                    qc.getTableLinks().insertEdge(t1, t2, qc.getRepresentative(rtt));
                    newlyGeneratedTables.add(qc.getRepresentative(rtt));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            }
        }
        return newlyGeneratedTables;
    }
}
