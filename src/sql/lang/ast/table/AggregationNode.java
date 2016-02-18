package sql.lang.ast.table;

import enumerator.parameterized.InstantiateEnv;
import javafx.util.Pair;
import sql.lang.DataType.*;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import sql.lang.trans.ValNodeSubstBinding;
import util.IndentionManagement;

import java.sql.Time;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/20/15.
 */
public class AggregationNode implements TableNode {

    public static String magicSeparatorSymbol = "-_-";

    List<String> aggrFields = new ArrayList<String>();
    List<Pair<String, Function<List<Value>, Value>>> targets = new ArrayList<>();
    TableNode tn;

    public AggregationNode(TableNode tn,
                           List<String> fields,
                           List<Pair<String, Function<List<Value>,Value>>> targets) {
        this.tn = tn;
        this.aggrFields = fields;
        this.targets = targets;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {

        // result table information
        Table resultTable = new Table();
        resultTable.updateName("anonymous");
        List<String> resultMeta = new ArrayList<String>();
        for (String s : this.aggrFields)
            resultMeta.add(s);
        for (Pair<String, Function<List<Value>, Value>> t : targets) {
            resultMeta.add("aggr-" + t.getKey());
        }
        List<TableRow> resultTableContent = new ArrayList<TableRow>();

        // eval the table to be aggregated
        Table tbl = tn.eval(env);

        // indices for aggregation fields
        List<Integer> indices = new ArrayList<Integer>();
        for (String s : aggrFields) {
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
                if (tempV == null)
                    throw new SQLEvalException("Aggregation function application error");
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
        schema.addAll(this.aggrFields);
        for (Pair<String, Function<List<Value>,Value>> t : targets) {
            schema.add(FuncName(t.getValue()) + magicSeparatorSymbol + t.getKey());
        }
        return schema;
    }

    public List<String> getFields() {
        return this.aggrFields;
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
        nameToRetrieve.addAll(this.aggrFields);
        for (Pair<String, Function<List<Value>, Value>> t : targets) {
            nameToRetrieve.add(t.getKey());
        }

        List<ValType> schemaTypes = new ArrayList<>();

        // TODO: think about naming stuff
        if (tn.getTableName().equals("anonymous")) {
            for (String n : this.aggrFields) {
                for (int i = 0; i < tableSchema.size(); i ++) {
                    if (tableSchema.get(i).equals(n.substring(n.indexOf(".") + 1))) {
                        schemaTypes.add(tableSchemaType.get(i));
                        break;
                    }
                }
            }
            for (Pair<String, Function<List<Value>, Value>> t : targets) {
                for (int i = 0; i < tableSchema.size(); i ++) {
                    if (tableSchema.get(i).equals(t.getKey().substring(t.getKey().indexOf(".") + 1))) {
                        if (!t.getValue().equals(AggrCount)) {
                            schemaTypes.add(tableSchemaType.get(i));
                        } else {
                            // if the aggregation function is COUNT,
                            // the type of the aggregation field will be changed to NumberVal
                            schemaTypes.add(ValType.NumberVal);
                        }
                        break;
                    }
                }
            }
        } else {
            for (String n : this.aggrFields) {
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
                        if (!t.getValue().equals(AggrCount)) {
                            schemaTypes.add(tableSchemaType.get(i));
                        } else {
                            // if the aggregation function is COUNT,
                            // the type of the aggregation field will be changed to NumberVal
                            schemaTypes.add(ValType.NumberVal);
                        }
                        break;
                    }
                }
            }
        }

        return schemaTypes;
    }

    public int getAggrFieldSize() {
        return this.aggrFields.size();
    }

    @Override
    public int getNestedQueryLevel() {
        return tn.getNestedQueryLevel() + 1;
    }

    @Override
    public String prettyPrint(int indentLv) {
        String result = "SELECT\r\n" + IndentionManagement.basicIndent();
        for (String f : aggrFields) {
            result += f + ", ";
        }
        for (int i = 0; i < targets.size(); i ++) {
            Pair<String, Function<List<Value>, Value>> t = targets.get(i);
            if (i != targets.size() - 1)
                result += FuncName(t.getValue()) + "(" + t.getKey() + "), ";
            else {
                // the last element
                result += FuncName(t.getValue()) + "(" + t.getKey() + ")\r\n";
            }
        }
        result += "FROM\r\n";
        result += this.tn.prettyPrint(1);
        result += "\r\nGROUP BY\r\n";
        boolean flag = true;
        for (String f : aggrFields) {
            if (flag == true)
                result += IndentionManagement.basicIndent() + f;
            else result += ", " + f;
        }
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
        if (l.get(0) instanceof NumberVal) {
            Double result = 0.;
            for (Value v : l) {
                result += ((NumberVal)v).getVal();
            }
            return new NumberVal(result);
        }
        System.err.println("[Error@AggregationNode26] aggregation performed on unexpected type");
        return null;
    };

    public static Function<List<Value>, Value> AggrMax = l -> {
        if (l.isEmpty()) {
            System.err.println("[Error@AggregationNode35] aggregation list is empty");
        }
        if (l.get(0) instanceof NumberVal) {
            Double maxVal = (Double) l.get(0).getVal();
            for (Value v : l) {
                if (((NumberVal) v).getVal() > maxVal) {
                    maxVal = (Double) v.getVal();
                }
            }
            return new NumberVal(maxVal);
        }
        if (l.get(0) instanceof DateVal) {
            Date maxDate = (Date)((Date)l.get(0).getVal()).clone();
            for (Value v : l) {
                if (((DateVal)v).getVal().compareTo(maxDate) > 0) {
                    maxDate = (Date)((Date) v.getVal()).clone();
                }
            }
            return new DateVal(maxDate);
        }
        if (l.get(0) instanceof TimeVal) {
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
        System.err.println("[Error@AggregationNode81] aggregation performed on unexpected type");
        return null;
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
        if (l.get(0) instanceof StringVal) {
            String result = "";
            for (Value sv : l) {
                result += sv.getVal() + ", ";
            }
            return new StringVal(result);
        }
        System.err.println("[Error@AggregationNode110] aggregation on wrong type");
        return null;
    };

    public static Function<List<Value>, Value> AggrCount = l -> new NumberVal(l.size());

    private static String FuncName(Function<List<Value>, Value> f) {
        if (f.equals(AggrSum))
            return "SUM";
        else if (f.equals(AggrAvg))
            return "AVG";
        else if (f.equals(AggrConcat))
            return "CONCAT";
        else if (f.equals(AggrCount))
            return "COUNT";
        else if (f.equals(AggrMax))
            return "MAX";
        else if (f.equals(AggrMin))
            return "MIN";
        else
            return "AGRREGATION";
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
            aggrFuncs.add(AggrMax);
            aggrFuncs.add(AggrMin);
            aggrFuncs.add(AggrSum);
        } else if (type.equals(ValType.DateVal)) {
            aggrFuncs.add(AggrCount);
            aggrFuncs.add(AggrMax);
            aggrFuncs.add(AggrMin);
        } else if (type.equals(ValType.StringVal)) {
            aggrFuncs.add(AggrCount);
            aggrFuncs.add(AggrConcat);
        } else if (type.equals(ValType.TimeVal)) {
            aggrFuncs.add(AggrCount);
            aggrFuncs.add(AggrMax);
            aggrFuncs.add(AggrMin);
        }
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
                this.aggrFields,
                this.targets);
    }

    @Override
    public TableNode substNamedVal(ValNodeSubstBinding vnsb) {
        return new AggregationNode(tn.substNamedVal(vnsb), this.aggrFields, this.targets);
    }

    @Override
    public List<NamedTable> namedTableInvolved() {
        return tn.namedTableInvolved();
    }

    @Override
    public TableNode tableSubst(List<Pair<TableNode,TableNode>> pairs) {
        return this;
    }

}
