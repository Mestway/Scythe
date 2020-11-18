package sql.lang.ast.table;

import forward_enumeration.primitive.parameterized.InstantiateEnv;
import util.Pair;
import sql.lang.datatype.*;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;
import util.RenameTNWrapper;

import java.sql.Time;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/20/15.
 */
public class AggregationNode extends TableNode {

    public static String magicSeparatorSymbol = "_";

    List<String> groupbyColumns = new ArrayList<String>();
    List<Pair<String, Function<List<Value>, Value>>> targets = new ArrayList<>();
    TableNode tn;

    public AggregationNode(TableNode tn,
                           List<String> fields,
                           List<Pair<String, Function<List<Value>,Value>>> targets) {
        this.tn = tn;
        this.groupbyColumns = fields;
        this.targets = targets;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {

        // result table information
        Table resultTable = new Table();
        resultTable.updateName("anonymous");
        List<String> resultMeta = new ArrayList<String>();
        for (String s : this.groupbyColumns)
            resultMeta.add(s);
        for (Pair<String, Function<List<Value>, Value>> t : targets) {
            resultMeta.add("aggr-" + t.getKey());
        }
        List<TableRow> resultTableContent = new ArrayList<TableRow>();

        // eval the table to be aggregated
        Table tbl = tn.eval(env);

        // indices for aggregation fields
        List<Integer> indices = new ArrayList<Integer>();
        for (String s : groupbyColumns) {
            indices.add(tbl.retrieveIndex(s));
        }
        // indices for aggregation targets
        List<Integer> targetIndices = this.targets.stream()
                .map(t -> tbl.retrieveIndex(t.getKey())).collect(Collectors.toList());

        Set<Integer> alreadyCollected = new HashSet<Integer>();

        for (int i = 0; i < tbl.getContent().size(); i ++) {
            TableRow currentRow = tbl.getContent().get(i);
            // initialiaze a container to contain these nodes
            List<List<Value>> aggregationTargetLists = new ArrayList<>();
            for (int k = 0; k < targetIndices.size(); k ++)
                aggregationTargetLists.add(new ArrayList<>());

            List<Value> valuesForTheRow = currentRow.retrieveValuesByIndices(indices);

            // the row is already collected
            if (alreadyCollected.contains(i))
                continue;

            // collect the current row
            for (int k = 0; k < targetIndices.size(); k ++) {
                aggregationTargetLists.get(k).add(currentRow.getValue(targetIndices.get(k)));
            }
            alreadyCollected.add(i);

            for (int j = i + 1; j < tbl.getContent().size(); j ++) {
                if (ListValEqual(tbl.getContent().get(j).retrieveValuesByIndices(indices), valuesForTheRow)) {
                    for (int k = 0; k < targetIndices.size(); k ++) {
                        aggregationTargetLists.get(k).add(tbl.getContent().get(j).getValue(targetIndices.get(k)));
                    }
                    alreadyCollected.add(j);
                }
            }

            List<Value> agrResultList = new ArrayList<>();

            for (int k = 0; k < this.targets.size(); k ++) {
                Value tempV = targets.get(k).getValue().apply(aggregationTargetLists.get(k));
                if (tempV == null) {
                    throw new SQLEvalException("Aggregation function application error");
                }
                agrResultList.add(tempV);
            }
            List<Value> rowContent = new ArrayList<Value>();
            for (Value v : valuesForTheRow) {
                rowContent.add(v);
            }
            rowContent.addAll(agrResultList);

            TableRow newTableRow = TableRow.TableRowFromContent(resultTable.getName(), resultMeta, rowContent);
            resultTableContent.add(newTableRow);
        }

        resultTable.initialize("anonymous", resultMeta, resultTableContent);
        return resultTable;
    }

    @Override
    public List<String> getSchema() {
        List<String> schema = new ArrayList<String>();
        schema.addAll(this.groupbyColumns);
        for (Pair<String, Function<List<Value>,Value>> t : targets) {
            String candidateName = FuncName(t.getValue()).toLowerCase() + magicSeparatorSymbol + t.getKey().substring(t.getKey().lastIndexOf(".") + 1);
            if (!schema.contains(candidateName))
                schema.add(candidateName);
            else {
                int i = 0;
                while (schema.contains(candidateName + i))
                    i ++;
                schema.add(candidateName + i);
            }
        }
        return schema;
    }

    public List<String> getFields() {
        return this.groupbyColumns;
    }

    @Override
    public String getTableName() {
        return "anonymous";
    }

    @Override
    public List<ValType> getSchemaType() {
        List<String> tableSchema = this.tn.getSchema();
        List<ValType> tableSchemaType = this.tn.getSchemaType();

        List<String> nameToRetrieve = new ArrayList<>();
        nameToRetrieve.addAll(this.groupbyColumns);
        for (Pair<String, Function<List<Value>, Value>> t : targets) {
            nameToRetrieve.add(t.getKey());
        }

        List<ValType> schemaTypes = new ArrayList<>();

            for (String n : this.groupbyColumns) {
                for (int i = 0; i < tableSchema.size(); i ++) {
                    if (tableSchema.get(i).equals(n)) {
                        schemaTypes.add(tableSchemaType.get(i));
                        break;
                    }
                }
            }
            for (Pair<String, Function<List<Value>, Value>> t : targets) {
                for (int i = 0; i < tableSchema.size(); i ++) {
                    if (tableSchema.get(i).equals(t.getKey())) {
                        if (!(t.getValue().equals(AggrCount) || t.getValue().equals(AggrCountDistinct))) {
                            schemaTypes.add(tableSchemaType.get(i));
                        } else {
                            // if the aggregation function is COUNT or COUNT-DISTINCT,
                            // the type of the aggregation field will be changed to NumberVal
                            schemaTypes.add(ValType.NumberVal);
                        }
                        break;
                    }
                }
            }

        return schemaTypes;
    }

    public int getAggrFieldSize() {
        return this.groupbyColumns.size();
    }

    @Override
    public int getNestedQueryLevel() {
        return tn.getNestedQueryLevel() + 1;
    }

    @Override
    public String prettyPrint(int indentLv, boolean asSubquery) {
        String result = "Select\r\n" + IndentionManagement.basicIndent();
        for (String f : groupbyColumns) {
            result += f + ", ";
        }
        for (int i = 0; i < targets.size(); i ++) {
            Pair<String, Function<List<Value>, Value>> t = targets.get(i);
            result += FuncName(t.getValue()) + "(" + t.getKey() + ") As " + this.getSchema().get(this.groupbyColumns.size() + i);
            if (i != targets.size() - 1)
                result +=  ", ";
            else {
                // the last element
                result +=  "\r\n";
            }
        }
        result += "From\r\n";
        result += this.tn.prettyPrint(1, true);

        if (! groupbyColumns.isEmpty()) {
            result += "\r\nGroup By\r\n";
            boolean flag = true;
            for (String f : groupbyColumns) {
                if (flag == true) {
                    result += IndentionManagement.basicIndent() + f;
                    flag = false;
                }
                else result += ", " + f;
            }
        }
        if (asSubquery)
            result = "(" + result + ")";
        return IndentionManagement.addIndention(result, indentLv);
    }

    // Given a table and a list of fields, see how many groups can be generated from grouping-by operator
    public static int numberOfGroups(Table table, List<String> fields) {
        int groupCount = 0;

        List<Integer> indices = new ArrayList<Integer>();
        for (String s : fields) {
            indices.add(table.retrieveIndex(s));
        }
        Set<Integer> alreadyCollected = new HashSet<Integer>();

        for (int i = 0; i < table.getContent().size(); i ++) {
            TableRow currentRow = table.getContent().get(i);
            List<Value> valuesForTheRow = currentRow.retrieveValuesByIndices(indices);

            // the row is already collected
            if (alreadyCollected.contains(i))
                continue;

            groupCount ++;
            alreadyCollected.add(i);

            for (int j = i + 1; j < table.getContent().size(); j ++) {
                if (ListValEqual(table.getContent().get(j).retrieveValuesByIndices(indices), valuesForTheRow)) {
                    alreadyCollected.add(j);
                }
            }
        }
        return groupCount;
    }

    // evaluate whether two lists are equal
    private static boolean ListValEqual(List<Value> l1, List<Value> l2) {
        if (l1.size() != l2.size())
            return false;
        for (int i = 0; i < l1.size(); i ++) {
            if (!l1.get(i).equals(l2.get(i)))
                return false;
        }
        return true;
    }

    private boolean alreadyCollected(List<List<Value>> collected, List<Value> current) {
        for (List<Value> lv : collected) {
            if (ListValEqual(lv, current))
                return true;
        }
        return false;
    }

    public static Function<List<Value>, Value> AggrSum = l -> {
        if (l.isEmpty()) {
            System.err.println("[Error@AggregationNode17] aggregation list is empty");
        }

        Double result = 0.;
        for (Value v : l) {
            if (v instanceof NullVal)
                continue;
            else if (v instanceof  NumberVal)
                result += ((NumberVal)v).getVal();
            else {
                System.err.println("[Error@AggregationNode26] aggregation performed on unexpected type");
                return null;
            }
        }
        return new NumberVal(result);


    };

    public static Function<List<Value>, Value> AggrMax = l -> {
        if (l.isEmpty()) {
            System.err.println("[Error@AggregationNode35] aggregation list is empty");
        }
        if (l.get(0).getValType().equals(ValType.NumberVal)) {
            if (l.get(0) instanceof NullVal)
                return  l.get(0);
            Double maxVal = (Double) l.get(0).getVal();
            for (Value v : l) {
                if (v instanceof NullVal)
                    return v;
                if (((NumberVal) v).getVal() > maxVal) {
                    maxVal = (Double) v.getVal();
                }
            }
            return new NumberVal(maxVal);
        }
        if (l.get(0).getValType().equals(ValType.DateVal)) {
            if (l.get(0) instanceof NullVal)
                return  l.get(0);
            Date maxDate = (Date)((Date)l.get(0).getVal()).clone();
            for (Value v : l) {
                if (((DateVal)v).getVal().compareTo(maxDate) > 0) {
                    maxDate = (Date)((Date) v.getVal()).clone();
                }
            }
            return new DateVal(maxDate);
        }
        if (l.get(0).getValType().equals(ValType.TimeVal)) {
            if (l.get(0) instanceof NullVal)
                return  l.get(0);
            Time maxTime = (Time)((Date)l.get(0).getVal()).clone();
            for (Value v : l) {
                if (((TimeVal)v).getVal().compareTo(maxTime) > 0) {
                    maxTime = (Time)((Time) v.getVal()).clone();
                }
            }
            return new DateVal(maxTime);
        }
        System.err.println("[Error@AggregationNode55] aggregation performed on unexpected type");
        return null;
    };

    public static Function<List<Value>, Value> AggrMin = l -> {
        if (l.isEmpty()) {
            System.err.println("[Error@AggregationNode61] aggregation list is empty");
        }
        if (l.get(0) instanceof NumberVal) {
            Double maxVal = (Double) l.get(0).getVal();
            for (Value v : l) {
                if (((NumberVal) v).getVal() < maxVal) {
                    maxVal = (Double) v.getVal();
                }
            }
            return new NumberVal(maxVal);
        }
        if (l.get(0) instanceof DateVal) {
            Date maxDate = (Date)((Date)l.get(0).getVal()).clone();
            for (Value v : l) {
                if (((DateVal)v).getVal().compareTo(maxDate) < 0) {
                    maxDate = (Date)((Date) v.getVal()).clone();
                }
            }
            return new DateVal(maxDate);
        }
        if (l.get(0) instanceof TimeVal) {
            Time minTime = (Time)((Date)l.get(0).getVal()).clone();
            for (Value v : l) {
                if (((TimeVal)v).getVal().compareTo(minTime) < 0) {
                    minTime = (Time)((Time) v.getVal()).clone();
                }
            }
            return new DateVal(minTime);
        }
        if (l.get(0) instanceof NullVal) {
            return l.get(0);
        }
        return null;
    };

    public static Function<List<Value>, Value> AggrFirst = l -> {
        if (l.isEmpty()) {
            System.err.println("[Error@AggregationNode100] aggregation list is empty");
        }
        return l.get(0);
    };

    public static Function<List<Value>, Value> AggrAvg = l -> {
        if (AggrSum.apply(l) == null) {
            System.err.println("[Error@AggregationNode92] aggregation result null");
        }
        Double avg = (Double) AggrSum.apply(l).getVal() / ((double) l.size());
        return new NumberVal(avg);
    };

    public static Function<List<Value>, Value> AggrConcat = l -> {
        if (l.isEmpty()) {
            System.err.println("[Error@AggregationNode100] aggregation list is empty");
        }
        if (l.get(0) instanceof StringVal || l.get(0) instanceof NullVal) {
            String result = "";
            for (Value sv : l) {
                result += sv.getVal() + ", ";
            }
            return new StringVal(result.substring(0, result.length() - 2));
        }
        System.err.println("[Error@AggregationNode110] aggregation on wrong type");
        return null;
    };

    public static Function<List<Value>, Value> AggrConcat2 = l -> {
        if (l.isEmpty()) {
            System.err.println("[Error@AggregationNode100] aggregation list is empty");
        }
        if (l.get(0) instanceof StringVal || l.get(0) instanceof NullVal) {
            String result = "";
            for (Value sv : l) {
                result += sv.getVal() + " ";
            }
            return new StringVal(result.substring(0, result.length() - 1));
        }
        System.err.println("[Error@AggregationNode110] aggregation on wrong type");
        return null;
    };

    public static Function<List<Value>, Value> AggrCount = l -> new NumberVal(l.size());

    public static Function<List<Value>, Value> AggrCountDistinct = l -> {
        List<Value> distinctVals = new ArrayList<>();
        for (Value v : l) {
            boolean flag = false;
            for (Value vv : distinctVals) {
                if (vv.getVal().equals(v.getVal()))
                    flag = true;
            }
            if (!flag)
                distinctVals.add(v);
        }
        return new NumberVal(distinctVals.size());
    };

    public static String FuncName(Function<List<Value>, Value> f) {
        if (f.equals(AggrSum))
            return "Sum";
        else if (f.equals(AggrAvg))
            return "Avg";
        else if (f.equals(AggrConcat) || f.equals(AggrConcat2))
            return "Concat";
        else if (f.equals(AggrCount))
            return "Count";
        else if (f.equals(AggrCountDistinct))
            return "Count_distinct";
        else if (f.equals(AggrMax))
            return "Max";
        else if (f.equals(AggrMin))
            return "Min";
        else if (f.equals(AggrFirst))
            return "First";
        else
            return "Aggr";
    }

    /**
     * Give a data type, enumerate all of the aggregation functions of that type
     * @param type
     * @return all aggregation functions for suitable for that type
     */
    public static List<Function<List<Value>, Value>> getAllAggrFunctions(ValType type) {
        List<Function<List<Value>, Value>> aggrFuncs = new ArrayList<>();
        if (type.equals(ValType.NumberVal)) {
            aggrFuncs.add(AggrAvg);
            aggrFuncs.add(AggrCount);
            aggrFuncs.add(AggrCountDistinct);
            aggrFuncs.add(AggrMax);
            aggrFuncs.add(AggrMin);
            aggrFuncs.add(AggrSum);
            aggrFuncs.add(AggrFirst);
        } else if (type.equals(ValType.DateVal)) {
            aggrFuncs.add(AggrCount);
            aggrFuncs.add(AggrCountDistinct);
            aggrFuncs.add(AggrMax);
            aggrFuncs.add(AggrMin);
        } else if (type.equals(ValType.StringVal)) {
            aggrFuncs.add(AggrCount);
            aggrFuncs.add(AggrCountDistinct);
            aggrFuncs.add(AggrConcat);
            aggrFuncs.add(AggrConcat2);
            aggrFuncs.add(AggrFirst);
            aggrFuncs.add(AggrFirst);
        } else if (type.equals(ValType.TimeVal)) {
            aggrFuncs.add(AggrCount);
            aggrFuncs.add(AggrCountDistinct);
            aggrFuncs.add(AggrMax);
            aggrFuncs.add(AggrMin);
        }
        return aggrFuncs;
    }

    public static List<Function<List<Value>, Value>> getAllAggrFunctions() {
        List<Function<List<Value>, Value>> aggrFuncs = new ArrayList<>();
        aggrFuncs.add(AggrAvg);
        aggrFuncs.add(AggrCount);
        aggrFuncs.add(AggrCountDistinct);
        aggrFuncs.add(AggrMax);
        aggrFuncs.add(AggrMin);
        aggrFuncs.add(AggrSum);
        aggrFuncs.add(AggrFirst);
        aggrFuncs.add(AggrConcat);
        aggrFuncs.add(AggrConcat2);
        return aggrFuncs;
    }

    @Override
    public List<Hole> getAllHoles() {
        return this.tn.getAllHoles();
    }

    @Override
    public TableNode instantiate(InstantiateEnv env) {
        return new AggregationNode(
                tn.instantiate(env),
                this.groupbyColumns,
                this.targets);
    }

    @Override
    public TableNode substNamedVal(ValNodeSubstBinding vnsb) {
        return new AggregationNode(tn.substNamedVal(vnsb), this.groupbyColumns, this.targets);
    }

    @Override
    public List<NamedTable> namedTableInvolved() {
        return tn.namedTableInvolved();
    }

    @Override
    public TableNode tableSubst(List<Pair<TableNode,TableNode>> pairs) {


        TableNode core = this.tn.tableSubst(pairs);

        List<String> currentSchema = tn.getSchema();
        List<String> newSchema = core.getSchema();

        Map<String, String> nameMapping = new HashMap<>();
        for (int i = 0; i < currentSchema.size(); i++) {
            nameMapping.put(
                    currentSchema.get(i),
                    newSchema.get(i));
        }

        List<String> newGroupbyColumns = new ArrayList<String>();
        for (String s : groupbyColumns) {
            newGroupbyColumns.add(nameMapping.get(s));
        }
        List<Pair<String, Function<List<Value>, Value>>> newTargets = new ArrayList<>();
        for (Pair<String, Function<List<Value>, Value>> p : targets) {
            newTargets.add(new Pair<>(nameMapping.get(p.getKey()), p.getValue()));
        }

        TableNode an = new AggregationNode(
                core,
                newGroupbyColumns, newTargets);

        return an;
    }

    public TableNode substCoreTable(TableNode newCore) {

        TableNode rt = RenameTNWrapper.tryRename(newCore);

        // rename the filters so that the filters refer to the elements in the new table.
        List<Pair<String, String>> stringNameBinding = new ArrayList<>();
        for (int i = 0; i < rt.getSchema().size(); i ++) {
            stringNameBinding.add(new Pair<>(
                    this.tn.getSchema().get(i),
                    rt.getSchema().get(i)));
        }

        List<String> newGroupByFields = new ArrayList<>();
        List<Pair<String, Function<List<Value>, Value>>> newTargets = new ArrayList<>();

        for (String s : groupbyColumns) {
            for (Pair<String, String> p : stringNameBinding) {
                if (p.getKey().equals(s)) {
                    newGroupByFields.add(p.getValue());
                    break;
                }
            }
        }

        for (Pair<String, Function<List<Value>, Value>> targetPair : targets) {
            for (Pair<String, String> p : stringNameBinding) {
                if (p.getKey().equals(targetPair.getKey())) {
                    newTargets.add(new Pair<>(p.getValue(), targetPair.getValue()));
                    break;
                }
            }
        }

        if (newGroupByFields.size() != groupbyColumns.size() || newTargets.size() != targets.size()) {
            System.out.println("[Fatal Error @ AggregationNode] failure to subsitute the core.");
        }

        TableNode newTN = new AggregationNode(rt, newGroupByFields, newTargets);
        return newTN;
    }

    @Override
    public List<String> originalColumnName() {

        List<String> innerQueryOriginalColName = this.tn.originalColumnName();
        List<String> result = new ArrayList<>();
        for (String s : this.groupbyColumns)
            result.add(innerQueryOriginalColName.get(this.tn.getSchema().indexOf(s)));

        for (Pair<String, Function<List<Value>, Value>> p : this.targets) {
            result.add(innerQueryOriginalColName.get(this.tn.getSchema().indexOf(p.getKey())));
        }
        return result;
    }

    @Override
    public double estimateAllFilterCost() {
        return tn.estimateAllFilterCost();
    }

    @Override
    public List<Value> getAllConstants() {
        return tn.getAllConstants();
    }

    @Override
    public String getQuerySkeleton() {
        return "(A " + this.tn.getQuerySkeleton() + ")";
    }
}
