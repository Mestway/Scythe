package enumerator.hueristics;

import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.VVComparator;
import sql.lang.ast.table.JoinNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.exception.SQLEvalException;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/24/16.
 */
public class NaturalJoinInference {

    // enumerate possible join conditions of the tables (with heuristic)
    public static List<TableNode> naturalJoinAll(List<Table> tables) {

        if (tables.isEmpty())
            return new ArrayList<>();

        List<TableNode> currentJoinedTable = new ArrayList<>();
        currentJoinedTable.add(new NamedTable(tables.get(0)));
        for (int i = 1; i < tables.size(); i ++) {
            List<TableNode> nextJoinTables = new ArrayList<>();
            for (TableNode tn : currentJoinedTable) {
                List<TableNode> temp = tryNaturalJoinTable(tn, new NamedTable(tables.get(i)));
                nextJoinTables.addAll(temp);
            }
            currentJoinedTable = nextJoinTables;
        }

        return currentJoinedTable;
    }

    private static List<TableNode> tryNaturalJoinTable(TableNode tn1, TableNode tn2){
        Table t1, t2;

        try {
            t1 = tn1.eval(new Environment());
            t2 = tn2.eval(new Environment());
        } catch (SQLEvalException e) {
            System.out.println();
            return new ArrayList<>();
        }

        List<TableNode> resultTableNode = new ArrayList<>();

        for (int i = 0; i < t1.getMetadata().size(); i ++) {
            for (int j = 0; j < t2.getMetadata().size(); j ++) {
                // if their column types are not compatible, they can't be the join key
                if (!t1.getSchemaType().get(i).equals(t2.getSchemaType().get(j))) {
                    continue;
                }
                // A heuristic strategy:
                // if these two columns are not similar at all, we can probably ignore the join key
                if (!columnNameSimilar(t1.getMetadata().get(i), t2.getMetadata().get(j))) {
                    continue;
                }

                Table joinTable = natJoinTwo(t1, t2, i, j);
                if (!joinTable.getContent().isEmpty()) {

                    String ignoredField = tn2.getSchema().get(j);

                    TableNode joinNode = new JoinNode(Arrays.asList(tn1, tn2));

                    TableNode tn = new SelectNode(
                            joinNode.getSchema().stream()
                                    .filter(m -> (!m.equals(ignoredField)))
                                    .map(m -> new NamedVal(m))
                                    .collect(Collectors.toList()),
                            joinNode,
                            new VVComparator(
                                    Arrays.asList(
                                            new NamedVal(joinNode.getSchema().get(i)),
                                            new NamedVal(joinNode.getSchema().get(t1.getMetadata().size() + j))
                                    ),
                                    VVComparator.eq));

                    resultTableNode.add(tn);
                }
            }
        }

        return resultTableNode;
    }

    private static boolean columnNameSimilar(String cn1, String cn2) {
        return true;
    }

    private static Table natJoinTwo(Table t1, Table t2, int t1Key, int t2Key) {
        List<String> newMeta = new ArrayList<>();
        for (int i = 0; i < t1.getMetadata().size(); i ++) {
            newMeta.add(t1.getName() + "." + t1.getMetadata().get(i));
        }
        for (int j = 0; j < t2.getMetadata().size(); j ++) {
            if (j == t2Key) {
                // we have already add the corresponding column in table 1
                continue;
            }
            newMeta.add(t2.getName() + "." + t2.getMetadata().get(j));
        }
        String newName = t1.getName() + t1Key + t2.getName() + t2Key;

        List<TableRow> tcs = new ArrayList<>();


        for (TableRow tr1 : t1.getContent()) {
            for (TableRow tr2 : t2.getContent()) {
                if (tr1.getValue(t1Key).equals(tr2.getValue(t2Key))) {
                    tcs.add(TableRow.TableRowFromContent(
                            newName,
                            newMeta,
                            connectRow(
                                    tr1.getValues(),
                                    tr2.getValues(),
                                    t1Key, t2Key)));
                }
            }
        }

        Table resultTable = new Table();
        resultTable.initialize(newName, newMeta, tcs);
        return resultTable;
    }

    private static List<Value> connectRow(List<Value> l1, List<Value> l2, int k1, int k2) {
        List<Value> result = new ArrayList<>();
        for (int i = 0; i < l1.size(); i ++) {
            result.add(l1.get(i));
        }
        for (int j = 0; j < l2.size(); j ++) {
            if (j == k2) continue;
            result.add(l2.get(j));
        }
        return result;
    }

}
