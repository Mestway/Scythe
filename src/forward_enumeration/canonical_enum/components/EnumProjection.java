package forward_enumeration.canonical_enum.components;

import forward_enumeration.context.EnumContext;
import forward_enumeration.container.QueryContainer;
import backward_inference.CellToCellMap;
import backward_inference.MappingInference;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.EmptyFilter;
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
    public static List<TableNode> enumProjection(List<TableNode> tableNodes, Table outputTable) {

        List<TableNode> result = new ArrayList<>();

        for (TableNode tn : tableNodes) {

            Table t;
            try {
                t = tn.eval(new Environment());
                // when the table row size does't equal to the size of output table,
                // it can not be obtained from projection.
                if (t.getContent().size() != outputTable.getContent().size())
                    continue;
            } catch (SQLEvalException e) {
                continue;
            }

            MappingInference mi = MappingInference.buildMapping(t, outputTable);
            List<CellToCellMap> maps = mi.genMappingInstances();

            List<List<ValNode>> lvns =  new ArrayList<>();
            for (CellToCellMap m : maps) {
                List<ValNode> selectNodes = new ArrayList<>();
                for (int j = 0; j < m.getMap().get(0).size(); j ++) {
                    selectNodes.add(new NamedVal(tn.getSchema().get(m.getMap().get(0).get(j).c())));
                }
                lvns.add(selectNodes);
            }

            for (List<ValNode> lvn : lvns) {
                SelectNode sn = new SelectNode(lvn, tn, new EmptyFilter());
                try {
                    Table tsn = sn.eval(new Environment());
                    if (tsn.contentStrictEquals(outputTable)) {
                        result.add(sn);
                    }
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    // projection enumeration only happens at the last step
    // the return value identifies whether the table is a runner-up
    public static boolean emitEnumProjection(List<TableNode> tableNodes, Table outputTable, QueryContainer qc) {

        boolean findone = false;

        List<TableNode> result = new ArrayList<>();

        for (TableNode tn : tableNodes) {

            Table t;
            try {
                t = tn.eval(new Environment());
                if (t.getContent().size() != outputTable.getContent().size())
                    continue;
            } catch (SQLEvalException e) {
                continue;
            }

            // Using coordinate map based inference to efficiently maintain result.
            MappingInference mi = MappingInference.buildMapping(t, outputTable);
            List<CellToCellMap> maps = mi.genMappingInstances();

            List<List<ValNode>> lvns =  new ArrayList<>();
            for (CellToCellMap m : maps) {
                List<ValNode> selectNodes = new ArrayList<>();
                for (int j = 0; j < m.getMap().get(0).size(); j ++)
                    selectNodes.add(new NamedVal(tn.getSchema().get(m.getMap().get(0).get(j).c())));
                lvns.add(selectNodes);
            }

            if (lvns.size() > 0) {
                findone = true;
            }

            for (List<ValNode> lvn : lvns) {
                SelectNode sn = new SelectNode(lvn, tn, new EmptyFilter());
                try {
                    Table tsn = sn.eval(new Environment());
                    if (tsn.contentStrictEquals(outputTable)) {
                        result.add(sn);
                        qc.insertQuery(sn);
                        qc.getTableLinks().insertEdge(
                                qc.getRepresentative(t),
                                qc.getRepresentative(tsn));
                    }
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
            }
        }

        return findone;
    }

    // Enumerate all possible combinations of selection fields of a select query
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
