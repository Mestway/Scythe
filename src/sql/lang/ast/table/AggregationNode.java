package sql.lang.ast.table;

import enumerator.parameterized.InstantiateEnv;
import sql.lang.DataType.*;
import sql.lang.Table;
import sql.lang.TableRow;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import util.IndentionManagement;

import java.sql.Time;
import java.util.*;
import java.util.function.Function;

/**
 * Created by clwang on 12/20/15.
 */
public class AggregationNode implements TableNode {

    public static String magicSeparatorSymbol = "-_-";

    String target;
    List<String> fields = new ArrayList<String>();

    TableNode tn;
    Function<List<Value>, Value> aggrFunction;

    public AggregationNode(Function<List<Value>, Value> aggrFunction,
                           TableNode tn,
                           List<String> fields,
                           String target) {
        this.tn = tn;
        this.aggrFunction = aggrFunction;
        this.fields = fields;
        this.target = target;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {

        // result table information
        Table resultTable = new Table();
        resultTable.updateName("anonymous");
        List<String> resultMeta = new ArrayList<String>();
        for (String s : this.fields)
            resultMeta.add(s);
        resultMeta.add("aggr-" + this.target);
        List<TableRow> resultTableContent = new ArrayList<TableRow>();

        // eval the table to be aggregated
        Table tbl = tn.eval(env);

        // indices for aggregation fields
        List<Integer> indices = new ArrayList<Integer>();
        for (String s : fields) {
            indices.add(tbl.retrieveIndex(s));
        }
        // indices for aggregation target
        Integer targetIndex = tbl.retrieveIndex(this.target);

        Set<Integer> alreadyCollected = new HashSet<Integer>();

        for (int i = 0; i < tbl.getContent().size(); i ++) {
            TableRow currentRow = tbl.getContent().get(i);
            List<Value> aggregationTargets = new ArrayList<Value>();
            List<Value> valuesForTheRow = currentRow.retrieveValuesByIndices(indices);

            // the row is already collected
            if (alreadyCollected.contains(i))
                continue;

            alreadyCollected.add(i);
            aggregationTargets.add(currentRow.getValue(targetIndex));

            for (int j = i + 1; j < tbl.getContent().size(); j ++) {
                if (ListValEqual(tbl.getContent().get(j).retrieveValuesByIndices(indices), valuesForTheRow)) {
                    aggregationTargets.add(tbl.getContent().get(j).getValue(targetIndex));
                    alreadyCollected.add(j);
                }
            }

            Value agrResult = this.aggrFunction.apply(aggregationTargets);
            if (agrResult == null)
                throw new SQLEvalException("Aggregation function application error");

            List<Value> rowContent = new ArrayList<Value>();
            for (Value v : valuesForTheRow) {
                rowContent.add(v);
            }
            rowContent.add(agrResult);

            TableRow newTableRow = TableRow.TableRowFromContent(resultTable.getName(), resultMeta, rowContent);
            resultTableContent.add(newTableRow);
        }

        resultTable.initialize("anonymous", resultMeta, resultTableContent);
        return resultTable;
    }

    @Override
    public List<String> getSchema() {
        List<String> schema = new ArrayList<String>();
        schema.addAll(this.fields);
        schema.add(FuncName(this.aggrFunction) + magicSeparatorSymbol + this.target);
        return schema;
    }

    public List<String> getFields() {
        return this.fields;
    }

    @Override
    public String getTableName() {
        return "anonymous";
    }

    @Override
    public List<ValType> getSchemaType() {
        List<String> tableSchema = this.tn.getSchema();
        List<ValType> tableSchemaType = this.tn.getSchemaType();

        List<String> nameToRetrieve = new ArrayList<String>();
        nameToRetrieve.addAll(this.fields);
        nameToRetrieve.add(this.target);

        List<ValType> schemaTypes = new ArrayList<ValType>();

        if (tn.getTableName().equals("anonymous")) {
            for (String n : nameToRetrieve) {
                for (int i = 0; i < tableSchema.size(); i ++) {
                    if (tableSchema.get(i).equals(n.substring(n.indexOf(".") + 1))) {
                        schemaTypes.add(tableSchemaType.get(i));
                        break;
                    }
                }
            }
        } else {
            for (String n : nameToRetrieve) {
                for (int i = 0; i < tableSchema.size(); i ++) {
                    if (tableSchema.get(i).equals(n)) {
                        schemaTypes.add(tableSchemaType.get(i));
                        break;
                    }
                }
            }
        }

        // if the aggregation function is COUNT,
        // the type of the aggregation field will be changed to NumberVal
        if (this.aggrFunction == AggrCount) {
            schemaTypes = schemaTypes.subList(0, schemaTypes.size() - 1);
            schemaTypes.add(ValType.NumberVal);
        }
        return schemaTypes;
    }

    @Override
    public int getNestedQueryLevel() {
        return tn.getNestedQueryLevel() + 1;
    }

    @Override
    public String prettyPrint(int indentLv) {
        String result = "SELECT\r\n" + IndentionManagement.basicIndent();
        for (String f : fields) {
            result += f + ", ";
        }
        result += FuncName(this.aggrFunction) + "(" + this.target + ")\r\n";
        result += "FROM\r\n";
        result += this.tn.prettyPrint(1);
        result += "\r\nGROUP BY\r\n";
        boolean flag = true;
        for (String f : fields) {
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
                this.aggrFunction,
                tn.instantiate(env),
                this.fields,
                target);
    }

}
