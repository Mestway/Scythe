package forward_enumeration.table_enumerator.hueristics;

import lang.sql.ast.valnode.ValNode;
import lang.sql.dataval.Value;
import lang.table.Table;
import lang.table.TableRow;
import lang.sql.ast.Environment;
import lang.sql.ast.predicate.BinopPred;
import lang.sql.ast.contable.JoinNode;
import lang.sql.ast.contable.NamedTableNode;
import lang.sql.ast.contable.SelectNode;
import lang.sql.ast.contable.TableNode;
import lang.sql.ast.valnode.NamedVal;
import lang.sql.exception.SQLEvalException;
import util.Pair;
import util.RenameWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/24/16.
 */
public class HeuristicNatJoin {

    // enumerate possible join conditions of the tables (with heuristic)
    public static List<TableNode> naturalJoinAll(List<Table> tables) {

        if (tables.isEmpty())
            return new ArrayList<>();

        List<TableNode> currentJoinedTable = new ArrayList<>();
        currentJoinedTable.add(new NamedTableNode(tables.get(0)));
        for (int i = 1; i < tables.size(); i ++) {
            List<TableNode> nextJoinTables = new ArrayList<>();
            for (TableNode tn : currentJoinedTable) {
                List<TableNode> temp = tryNaturalJoinTable(tn, new NamedTableNode(tables.get(i)));
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

        for (int i = 0; i < t1.getSchema().size(); i ++) {
            for (int j = 0; j < t2.getSchema().size(); j ++) {
                // if their column types are not compatible, they can't be the join key
                if (!t1.getSchemaType().get(i).equals(t2.getSchemaType().get(j))) {
                    continue;
                }
                // A heuristic strategy:
                // if these two columns are not similar at all, we can probably ignore the join key
                if (!columnNameSimilar(t1.getSchema().get(i), t2.getSchema().get(j))) {
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
                            new BinopPred(
                                    Arrays.asList(
                                            new NamedVal(joinNode.getSchema().get(i)),
                                            new NamedVal(joinNode.getSchema().get(t1.getSchema().size() + j))
                                    ),
                                    BinopPred.eq));

                    resultTableNode.add(tn);
                }
            }
        }

        return resultTableNode;
    }

    /**
     * try to infer the equi join key between two tables tn1 and tn2
     * @param tn1 table 1
     * @param tn2 table 2
     * @return a pair, the first element indicates whether we successfully find a equi join
     *          the second is the join result -- equi join result if succeed, other wise return the cartesian product
     */
    public static Pair<Boolean, TableNode> heuristicEquiJoinTwo(TableNode tn1, TableNode tn2) {
        Table t1, t2;
        try {
            t1 = tn1.eval(new Environment());
            t2 = tn2.eval(new Environment());
        } catch (SQLEvalException e) {
            t1 = null;
            t2 = null;
            e.printStackTrace();
        }

        for (int i = 0; i < t1.getSchema().size(); i ++) {
            for (int j = 0; j < t2.getSchema().size(); j ++) {
                if (t1.getSchemaType().get(i) == t2.getSchemaType().get(j)) {
                    int ind1 = i, ind2 = j;
                    List<Value> vals1 = t1.getContent()
                            .stream()
                            .map(c -> c.getValue(ind1))
                            .collect(Collectors.toList());
                    List<Value> vals2 = t2.getContent()
                            .stream()
                            .map(c -> c.getValue(ind2))
                            .collect(Collectors.toList());
                    int score = 0;
                    for (int k1 = 0; k1 < vals1.size(); k1 ++) {
                        for (int k2 = 0; k2 < vals2.size(); k2++) {
                            if (vals1.get(k1).equals(vals2.get(k2))) {
                                score++;
                            }
                        }
                    }
                    if ((score * 10) / ((vals1.size() + vals2.size()) /2) > 2) {
                        TableNode renamedJoin = RenameWrapper
                                .tryRename(new JoinNode(Arrays.asList(tn1, tn2)));

                        List<ValNode> newSchema = new ArrayList<>();
                        for (int k = 0; k < renamedJoin.getSchema().size(); k ++) {
                            if (k != j + t1.getSchema().size()) {
                                newSchema.add(new NamedVal(renamedJoin.getSchema().get(k)));
                            }
                        }
                        return new Pair<Boolean, TableNode>(true,new SelectNode(
                                newSchema,
                                renamedJoin,
                                new BinopPred(
                                        Arrays.asList(
                                                new NamedVal(renamedJoin.getSchema().get(i)),
                                                new NamedVal(renamedJoin.getSchema().get(t1.getSchema().size() + j))),
                                        BinopPred.eq
                                )));
                    }
                }
            }
        }
        return new Pair<>(false, RenameWrapper.tryRename(new JoinNode(Arrays.asList(tn1, tn2))));
    }

    private static boolean columnNameSimilar(String cn1, String cn2) {
        return true;
    }

    private static Table natJoinTwo(Table t1, Table t2, int t1Key, int t2Key) {
        List<String> newMeta = new ArrayList<>();
        for (int i = 0; i < t1.getSchema().size(); i ++) {
            newMeta.add(t1.getName() + "." + t1.getSchema().get(i));
        }
        for (int j = 0; j < t2.getSchema().size(); j ++) {
            if (j == t2Key) {
                // we have already add the corresponding column in table 1
                continue;
            }
            newMeta.add(t2.getName() + "." + t2.getSchema().get(j));
        }
        String newName = t1.getName() + t1Key + t2.getName() + t2Key;

        List<TableRow> tcs = new ArrayList<>();


        for (TableRow tr1 : t1.getContent()) {
            for (TableRow tr2 : t2.getContent()) {
                if (tr1.getValue(t1Key).equals(tr2.getValue(t2Key))) {
                    tcs.add(TableRow.TableRowFromContent(newMeta, connectRow(
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
