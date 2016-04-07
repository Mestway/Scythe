package enumerator.context;

import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import util.HierarchicalMap;
import util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A special light weight query chest to store one
 * step enumeration table and query relations.
 *
 * In contrast to the regular query chest, both table and query will be stored in this data structure,
 * and a method for exporting table to queries is designed
 * Created by clwang on 4/7/16.
 */
public class MiniQueryChest {

    private Map<Table, List<TableNode>> memory = new HierarchicalMap<>();

    public static MiniQueryChest initWithInputTables(List<Table> input) {
        MiniQueryChest miniqc = new MiniQueryChest();
        miniqc.updateQueries(input.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        return miniqc;
    }

    public void updateQueries(List<TableNode> queries) {
        for (TableNode tn : queries) {
            try {
                Table t = tn.eval(new Environment());

                if (t.getContent().size() == 0)
                    continue;

                if (memory.containsKey(t)) {
                    memory.get(t).add(tn);
                } else {
                    ArrayList<TableNode> ar = new ArrayList<>();
                    ar.add(tn);
                    memory.put(t, ar);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<TableNode> lookup(Table t) {
        if (memory.containsKey(t)) {
            return memory.get(t);
        }
        else return null;
    }

    /**
     * Export a table into a list of tables by unfolding intermediate results with memoized structures
     * @param tn the table node to be unfolded
     * @param alreadyLookedUp the set of named tables that are already used in unfolding
     * @param inputNamedTables the input table for the enumeration process, which determines the when to stop unfolding
     * @return the set of queries that are equivalent to the input tn after unfolding
     */
    public List<TableNode> export(TableNode tn, List<NamedTable> alreadyLookedUp, List<NamedTable> inputNamedTables) {

        List<TableNode> result = new ArrayList<>();

        List<NamedTable> namedTables = tn.namedTableInvolved();

        if (namedTables.isEmpty())
            return Arrays.asList(tn);

        List<NamedTable> tableToSubst = new ArrayList<>();

        for (NamedTable nt : namedTables) {
            boolean contained = false;
            for (NamedTable it : inputNamedTables) {
                if (it.getTable().contentEquals(nt.getTable())) {
                    contained = true;
                }
            }
            if (contained == false) {
                tableToSubst.add(nt);
            }
        }

        if (tableToSubst.isEmpty()) {
            // does not contain any other intermediate tables
            return Arrays.asList(tn);
        }

        for (NamedTable nt : namedTables) {
            for (NamedTable alt : alreadyLookedUp) {
                if (alt.getTable().contentEquals(nt.getTable()))
                    return Arrays.asList();
            }
        }

        List<NamedTable> newAlreadyLookedUp = new ArrayList<>();
        newAlreadyLookedUp.addAll(alreadyLookedUp);
        newAlreadyLookedUp.addAll(namedTables);

        List<List<TableNode>> targetedMaps = new ArrayList<>();
        for (NamedTable nt : tableToSubst) {
            List<TableNode> lkupResult = this.lookup(nt.getTable());
            List<TableNode> candidatesForNt = new ArrayList<>();
            lkupResult.stream().forEach(
                    lkupTn -> candidatesForNt.addAll(
                            this.export(lkupTn,
                                    newAlreadyLookedUp.stream().distinct().collect(Collectors.toList()),
                                    inputNamedTables)));
            targetedMaps.add(candidatesForNt);
        }

        List<List<TableNode>> ImageSet = MiniQueryChest.product(targetedMaps);

        for (List<TableNode> oneMap : ImageSet) {
            List<Pair<TableNode, TableNode>> substPair = new ArrayList<>();
            for (int i = 0; i < oneMap.size(); i ++) {
                substPair.add(new Pair<>(tableToSubst.get(i), oneMap.get(i)));
            }
            result.add(tn.tableSubst(substPair));
        }

        return result;
    }

    // given a list of tables, calculate the cartesian product of these tables
    public static List<List<TableNode>> product(List<List<TableNode>> tns) {
        List<List<TableNode>> result = new ArrayList<>();

        if (tns.size() == 1) {
            for (TableNode tn : tns.get(0)) {
                result.add(Arrays.asList(tn));
            }
            return result;
        }

        List<List<TableNode>> tailProd = product(tns.subList(1, tns.size()));
        for (TableNode tn : tns.get(0)) {
            for (List<TableNode> tailTN : tailProd) {
                ArrayList<TableNode> temp = new ArrayList();
                temp.add(tn);
                temp.addAll(tailTN);
                result.add(temp);
            }
        }
        return result;
    }

}
