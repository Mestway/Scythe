package forward_enumeration.canonical_enum.components;

import forward_enumeration.container.QueryContainer;
import lang.table.Table;
import lang.sql.ast.Environment;
import lang.sql.ast.contable.RenameTableNode;
import lang.sql.ast.contable.TableNode;
import lang.sql.ast.contable.UnionNode;
import lang.sql.exception.SQLEvalException;
import util.RenameWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

                RenameTableNode rt = (RenameTableNode) RenameWrapper.tryRename(jn);
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


                RenameTableNode rt = (RenameTableNode) RenameWrapper.tryRename(jn);
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
