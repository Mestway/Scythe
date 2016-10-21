package forward_enumeration.context;

import global.GlobalConfig;
import sql.lang.datatype.ValType;
import sql.lang.datatype.Value;
import sql.lang.Table;
import sql.lang.ast.table.*;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import util.DebugHelper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An environment used to store
 * The context is a data structure
 *  used to store constraint contexts
 *
 *  Whenever we want to enumerate something, we shall
 *  1) Create an enum context with
 * Created by clwang on 12/23/15.
 */
public class EnumContext {

    // used to store the input tables, which will never change for an enumeration process,
    // Given information for input tables and the output table for enumeration
    // The following fields will not mutate as they are meta-enumeration information
    private List<Table> inputs = new ArrayList<>();
    private Table outputTable = null;
    private List<TableNode> parameterizedTableNodes = new ArrayList<>();
    List<Function<List<Value>, Value>> aggrfunctions = new ArrayList<>();
    private int maxFilterLength = GlobalConfig.MAXIMUM_FILTER_LENGTH;

    // tableNodes are used to store tables that are used as input of current enumeration iteration.
    private List<TableNode> tableNodes = new ArrayList<>();
    private List<ValNode> valNodes = new ArrayList<>();

    //mapping a column name to its type
    private Map<String, ValType> colNameToType = new HashMap<>();

    public EnumContext() {}
    // initializing an EnumContext using tables and EnumConfig
    public EnumContext(List<Table> tbs, EnumConfig c) {
        this.inputs = tbs;
        this.tableNodes = tbs.stream().map(t -> new NamedTable(t)).collect(Collectors.toList());
        this.valNodes = c.constValNodes();
        this.aggrfunctions = c.getAggrFuns();
    }

    public void setValNodes(List<ValNode> vns) { this.valNodes = vns; }

    // set and get the number of maximum filter length
    public void setMaxFilterLength(int maxLength) { this.maxFilterLength = maxLength; }
    public int getMaxFilterLength() { return this.maxFilterLength; }

    public void setParameterizedTables(List<TableNode> tbs) { this.parameterizedTableNodes = tbs; }
    public List<TableNode> getParameterizedTables() { return this.parameterizedTableNodes; }

    // set and get input, output table for this enumeration context, this method is not used in most situations
    // the output tables are only able to be used in some cases
    public List<Table> getInputs() { return this.inputs; }
    public void setOutputTable(Table outputTable) { this.outputTable = outputTable; }
    public Table getOutputTable() { return this.outputTable; }

    public List<TableNode> getTableNodes() { return this.tableNodes; }
    public void setTableNodes(List<TableNode> tableNodes) { this.tableNodes = tableNodes; }

    // Extend the atomic value bindings (with their types) in the context,
    // a new deep-copy version of EnumContext will be generated.
    public static EnumContext extendValueBinding(
            EnumContext ec, Map<String, ValType> map) {
        EnumContext newEC = EnumContext.deepCopy(ec);
        for (Map.Entry<String, ValType> i : map.entrySet()) {
            newEC.valNodes.add(new NamedVal(i.getKey()));
        }
        newEC.colNameToType.putAll(ec.colNameToType);
        newEC.colNameToType.putAll(map);
        return newEC;
    }

    public List<ValNode> getValNodes() {
        // this implementation use deep copy to avoid messing up value nodes in the context
        List<ValNode> allValNodes = new ArrayList<ValNode>();
        allValNodes.addAll(this.valNodes);
        return allValNodes;
    }

    // get aggregation functions that are used in this enumeration context
    public List<Function<List<Value>, Value>> getAggrFuns(ValType type) {
        List<Function<List<Value>, Value>> fs = AggregationNode.getAllAggrFunctions(type);
        return fs.stream().filter(f -> this.aggrfunctions.contains(f)).collect(Collectors.toList());
    }

    public ValType getValType(String name) {
        return colNameToType.get(name);
    }

    public static EnumContext deepCopy(EnumContext ec) {
        EnumContext newEC = new EnumContext();
        // these value will not be shared due to
        // the fact the the enumerator in different stage have different value bindings
        newEC.valNodes.addAll(ec.valNodes);
        newEC.colNameToType.putAll(ec.colNameToType);

        // these fields are shared
        newEC.tableNodes = ec.tableNodes;
        newEC.aggrfunctions = ec.aggrfunctions;
        newEC.maxFilterLength = ec.maxFilterLength;
        newEC.parameterizedTableNodes = ec.parameterizedTableNodes;
        newEC.outputTable = ec.outputTable;
        newEC.inputs = ec.inputs;
        return newEC;
    }

    public void debugPrint() {
        System.out.println(" EC PRINT START ");
        System.out.println("~~==~~ tables");
        DebugHelper.printTableNodes(this.tableNodes);
        /*System.out.println("diffTables: " + this.tableNodes.size() + ", AllTables: " + this.memoizedTables.entrySet().stream().map(i -> i.getValue().size()).reduce(
                0,
                (a, b) -> a + b));*/
        System.out.println("~~==~~ valnodes");
        DebugHelper.printValNodes(this.valNodes);
        System.out.println(" EC PRINT END ");
    }

    public boolean isInputTableNode(TableNode tn) {
        if (! (tn instanceof NamedTable))
            return false;
        else {
            Table bj = ((NamedTable) tn).getTable();
            for (Table t : this.getInputs())
                if (bj.containsContent(t)) {
                    return true;
                }
        }
        return false;
    }

}
