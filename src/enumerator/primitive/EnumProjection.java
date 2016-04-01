package enumerator.primitive;

import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.EmptyFilter;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.CombinationGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enumerate projection table nodes given the enumeration context EC
 * Created by clwang on 1/26/16.
 */
public class EnumProjection {

    // projection enumeration only happens at the last step
    public static List<TableNode> enumProjection(EnumContext ec, Table outputTable) {
       // TODO: definitely exists a way to solve the correspondence between the nodes and the output example

        List<TableNode> tableNodes = ec.getTableNodes();
        List<TableNode> result = new ArrayList<>();

        for (TableNode tn : tableNodes) {

            try {
                Table t = tn.eval(new Environment());
                if (t.getContent().size() != outputTable.getContent().size())
                    continue;
            } catch (SQLEvalException e) {
                continue;
            }

            List<List<ValNode>> lvns = enumSelectArgs(tn, false);
            for (List<ValNode> lvn : lvns) {
                SelectNode sn = new SelectNode(lvn, tn, new EmptyFilter());
                try {
                    Table t = sn.eval(new Environment());
                    if (t.contentStrictEquals(outputTable)) {
                        result.add(sn);
                    }
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    // Enumerate the selection fields of a select query
    private static List<List<ValNode>> enumSelectArgs(TableNode tableNode, boolean enumStar) {
        List<ValNode> vals = new ArrayList<ValNode>();

        // collect table column names from the schema
        vals.addAll(tableNode.getSchema().stream()
                .map(s -> new NamedVal(s)).collect(Collectors.toList()));

        List<List<ValNode>> valNodes = new ArrayList<>();
        if (! enumStar)
            valNodes = CombinationGenerator.genCombination(vals);
        else
            valNodes.add(vals);

        return valNodes;
    }

}
