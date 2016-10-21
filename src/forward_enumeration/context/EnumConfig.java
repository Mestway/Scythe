package forward_enumeration.context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sql.lang.Table;
import sql.lang.datatype.Value;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.val.ConstantVal;
import sql.lang.ast.val.ValNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 1/7/16.
 * Configuration used in enumeration
 */
public class EnumConfig {

    // depth of the enumeration always starts at 2
    int maxDepth = 2;
    List<ValNode> constValNodes = new ArrayList<>();
    List<Function<List<Value>, Value>> aggrFuns = new ArrayList<>();

    // the maximum number of parameters allowed in Exists clause, if the number is 0, Exists will not be allowed
    int numberOfParam = 0;
    List<Table> existsCore = new ArrayList<>();

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public EnumConfig(int maxDepth,
                      List<Value> constants,
                      List<Function<List<Value>, Value>> aggrFuns,
                      int numberOfParam,
                      List<Table> existsCore) {
        this.maxDepth = maxDepth;
        this.constValNodes = constants.stream()
                .map(c -> new ConstantVal(c)).collect(Collectors.toList());
        this.aggrFuns = aggrFuns;
        this.numberOfParam = numberOfParam;
        this.existsCore.addAll(existsCore);
    }

    public int getNumberOfParam() { return this.numberOfParam; }
    public List<Table> getExistsCores() { return this.existsCore; }
    public List<Function<List<Value>, Value>> getAggrFuns() {
        return this.aggrFuns;
    }
    public List<ValNode> constValNodes() {
        return this.constValNodes;
    }
    public int maxDepth() { return this.maxDepth; }
    public void setExistsCore(int numberOfParam, List<Table> existsCore) {
        this.numberOfParam = numberOfParam;
        this.existsCore.addAll(existsCore);
    }

    private class JsonConstraint {
        List<String> constants;
        List<String> aggregation_functions;

        public JsonConstraint(String fileContent) {
            Gson gson = new GsonBuilder().create();
            JsonConstraint p = gson.fromJson(fileContent, JsonConstraint.class);
            this.constants = p.constants;
            this.aggregation_functions = p.aggregation_functions;
            if (constants == null) constants = new ArrayList<>();
            if (aggregation_functions == null) aggregation_functions = new ArrayList<>();
        }

        public String toString() {
            String s = "";
            s += "constants: " + constants.stream().reduce(String::concat) + "\n";
            s += "aggr: " + aggregation_functions.stream().reduce(String::concat) + "\n";
            return s;
        }
    }

    public EnumConfig(String fileContent) {
        JsonConstraint p = new JsonConstraint(fileContent);
        this.constValNodes = p.constants.stream()
                .map(c -> new ConstantVal(Value.parse(c))).collect(Collectors.toList());
        this.aggrFuns = p.aggregation_functions.stream().map(
                x -> {
                    switch (x) {
                        case "min": return AggregationNode.AggrMin;
                        case "max": return AggregationNode.AggrMax;
                        case "avg": return AggregationNode.AggrAvg;
                        case "count": return AggregationNode.AggrCount;
                        case "count-distinct": return AggregationNode.AggrCountDistinct;
                        case "concat": return AggregationNode.AggrConcat;
                        case "sum": return AggregationNode.AggrSum;
                        case "first": return AggregationNode.AggrFirst;
                        default: return null;
                    }
                }
        ).filter(x ->  x != null).collect(Collectors.toList());

        if (this.aggrFuns.contains(AggregationNode.AggrConcat))
            aggrFuns.add(AggregationNode.AggrConcat2);
    }
}
