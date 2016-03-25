package enumerator;

import javafx.util.Pair;
import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.DebugHelper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An environment used to store
 * The context is a data structure
 *  used to store constraint contexts
 * Created by clwang on 12/23/15.
 */
public class EnumContext {

    // enumeration information about output table
    private Table outputTable = null;

    // tabled that is memoized
    private Map<Table, List<TableNode>> memoizedTables = new HashMap<>();

    private int maxFilterLength = 2;

    // tableNodes are used to store tables that are used as input of current enumeration iteration.
    private List<TableNode> tableNodes = new ArrayList<>();
    private List<ValNode> valNodes = new ArrayList<>();

    // ** the only part of the context that should not mutate once intialized
    private List<TableNode> parameterizedTables = new ArrayList<>();
    List<Function<List<Value>, Value>> aggrfuns = new ArrayList<>();

    //mapping a column name to its corresponding type
    private Map<String, ValType> typeMap = new HashMap<>();

    // used to store the input tables
    private List<Table> inputs = new ArrayList<>();
    public List<Table> getInputs() { return this.inputs; }

    public EnumContext() {}
    // initializing an EnumContext using tables and constraint
    public EnumContext(List<Table> tbs, Constraint c) {
        inputs = tbs;
        this.updateTableNodes(tbs.stream().map(t -> new NamedTable(t)).collect(Collectors.toList()));
        valNodes = c.constValNodes();
        this.aggrfuns = c.getAggrFuns();
    }

    // update the enumContext adding new tables
    // (these new tables will be used in next rounds enumeration)
    public void updateTableNodes(List<TableNode> tns) {
        for (TableNode tn : tns) {
            try {
                Table t = tn.eval(new Environment());

                if (t.getContent().size() == 0)
                    continue;

                if (memoizedTables.containsKey(t)) {
                    memoizedTables.get(t).add(tn);
                } else {
                    ArrayList<TableNode> ar = new ArrayList<>();
                    ar.add(tn);
                    memoizedTables.put(t, ar);
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("[EnumContext71] TableNode not executable.");
                System.out.println(tn.prettyPrint(0));
                System.exit(-1);
            }
        }
        this.tableNodes = memoizedTables.keySet()
                .parallelStream().map(t -> new NamedTable(t)).collect(Collectors.toList());
    }

    public void setValNodes(List<ValNode> vns) { this.valNodes = vns; }

    // set and get the number of maxinum filter length
    public void setMaxFilterLength(int maxLength) { this.maxFilterLength = maxLength; }
    public int getMaxFilterLength() { return this.maxFilterLength; }

    // set and get parameterized tables used in enumeration
    public void setParameterizedTables(List<TableNode> tbs) { this.parameterizedTables = tbs; }
    public List<TableNode> getParameterizedTables() { return this.parameterizedTables; }

    // set and get output table for this enumeration context, this method is not used in most situations
    public void setOutputTable(Table outputTable) { this.outputTable = outputTable; }
    public Table getOutputTable() { return this.outputTable; }

    public Map<Table, List<TableNode>> getMemoizedTables() { return this.memoizedTables; }

    // Extend atomic tables in the context
    public static EnumContext extendTable(
            EnumContext ec, List<TableNode> tables) {
        EnumContext newEC = EnumContext.deepCopy(ec);
        newEC.updateTableNodes(tables);
        return newEC;
    }

    // Extend the atomic values in the context
    public static EnumContext extendTypeMap(
            EnumContext ec, Map<String, ValType> map) {
        EnumContext newEC = EnumContext.deepCopy(ec);
        for (Map.Entry<String, ValType> i : map.entrySet()) {
            newEC.valNodes.add(new NamedVal(i.getKey()));
        }
        newEC.typeMap.putAll(ec.typeMap);
        newEC.typeMap.putAll(map);
        return newEC;
    }

    public List<TableNode> getBasicTableNodes() {
        return this.tableNodes;
    }

    public List<TableNode> getTableNodes() {
        // get the first element of all entries in the memoizedTables map
        return this.memoizedTables.entrySet().parallelStream().map(e -> e.getValue().get(0)).collect(Collectors.toList());
    }

    public List<ValNode> getValNodes() {
        List<ValNode> allValNodes = new ArrayList<ValNode>();
        allValNodes.addAll(this.valNodes);
        return allValNodes;
    }

    // get aggregation functions that are used in this enumeration context
    public List<Function<List<Value>, Value>> getAggrFuns(ValType type) {
        List<Function<List<Value>, Value>> fs = AggregationNode.getAllAggrFunctions(type);
        return fs.stream().filter(f -> this.aggrfuns.contains(f)).collect(Collectors.toList());
    }

    public ValType getValType(String name) {
        return typeMap.get(name);
    }

    public static EnumContext deepCopy(EnumContext ec) {
        EnumContext newEC = new EnumContext();
        newEC.tableNodes.addAll(ec.tableNodes);
        newEC.aggrfuns.addAll(ec.aggrfuns);
        newEC.valNodes.addAll(ec.valNodes);
        newEC.typeMap.putAll(ec.typeMap);
        newEC.maxFilterLength = ec.maxFilterLength;
        newEC.parameterizedTables = ec.parameterizedTables;
        // this field is shared
        newEC.memoizedTables = ec.memoizedTables;
        newEC.outputTable = ec.outputTable;
        newEC.inputs = ec.inputs;
        return newEC;
    }

    public void debugPrint() {
        System.out.println(" EC PRINT START ");
        System.out.println("~~==~~ tables");
        //DebugHelper.printTableNodes(this.tableNodes);
        System.out.println("diffTables: " + this.tableNodes.size() + ", AllTables: " + this.memoizedTables.entrySet().stream().map(i -> i.getValue().size()).reduce(
                0,
                (a, b) -> a + b));
        System.out.println("~~==~~ valnodes");
        DebugHelper.printValNodes(this.valNodes);
        System.out.println(" EC PRINT END ");
    }

    public List<TableNode> lookup(Table t) {
        if (memoizedTables.containsKey(t))
            return memoizedTables.get(t);
        else return new ArrayList<>();
    }

    // Export a table into a list of tables by unfolding intermediate results with memoized structures
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

        List<List<TableNode>> ImageSet = product(targetedMaps);

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
