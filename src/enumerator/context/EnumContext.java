package enumerator.context;

import enumerator.Constraint;
import javafx.util.Pair;
import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import util.DebugHelper;
import util.HierarchicalMap;

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
        this.tableNodes = tbs.stream().map(t -> new NamedTable(t)).collect(Collectors.toList());
        valNodes = c.constValNodes();
        this.aggrfuns = c.getAggrFuns();
    }

    public void setValNodes(List<ValNode> vns) { this.valNodes = vns; }

    // set and get the number of maxinum filter length
    public void setMaxFilterLength(int maxLength) { this.maxFilterLength = maxLength; }
    public int getMaxFilterLength() { return this.maxFilterLength; }

    public void setParameterizedTables(List<TableNode> tbs) { this.parameterizedTables = tbs; }
    public List<TableNode> getParameterizedTables() { return this.parameterizedTables; }

    // set and get output table for this enumeration context, this method is not used in most situations
    public void setOutputTable(Table outputTable) { this.outputTable = outputTable; }
    public Table getOutputTable() { return this.outputTable; }

    public List<TableNode> getTableNodes() { return this.tableNodes;}
    public void setTableNodes(List<TableNode> tableNodes) { this.tableNodes = tableNodes; }


    // Extend atomic tables in the context
    public static EnumContext updateTableNodes(
            EnumContext ec, List<TableNode> tables) {
        EnumContext newEC = EnumContext.deepCopy(ec);
        newEC.tableNodes = tables;
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
        newEC.outputTable = ec.outputTable;
        newEC.inputs = ec.inputs;
        return newEC;
    }

    public void debugPrint() {
        System.out.println(" EC PRINT START ");
        System.out.println("~~==~~ tables");
        //DebugHelper.printTableNodes(this.tableNodes);
        /*System.out.println("diffTables: " + this.tableNodes.size() + ", AllTables: " + this.memoizedTables.entrySet().stream().map(i -> i.getValue().size()).reduce(
                0,
                (a, b) -> a + b));*/
        System.out.println("~~==~~ valnodes");
        DebugHelper.printValNodes(this.valNodes);
        System.out.println(" EC PRINT END ");
    }

}
