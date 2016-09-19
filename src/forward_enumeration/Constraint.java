package forward_enumeration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
 */
public class Constraint {
    int maxDepth = 2;
    List<ValNode> constValNodes = new ArrayList<>();
    List<Function<List<Value>, Value>> aggrFuns = new ArrayList<>();
    int numberOfParam = 2;
    int maxFilterLength = 2;


    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public Constraint(int maxDepth,
                      List<Value> constants,
                      List<Function<List<Value>, Value>> aggrFuns,
                      int numberOfParam) {
        this.maxDepth = maxDepth;
        this.constValNodes = constants.stream()
                .map(c -> new ConstantVal(c)).collect(Collectors.toList());
        this.aggrFuns = aggrFuns;
        this.numberOfParam = numberOfParam;
    }

    public Constraint(int maxDepth,
                      List<Value> constants,
                      List<Function<List<Value>, Value>> aggrFuns,
                      int numberOfParam,
                      int maxFilterLength) {
        this.maxDepth = maxDepth;
        this.constValNodes = constants.stream()
                .map(c -> new ConstantVal(c)).collect(Collectors.toList());
        this.aggrFuns = aggrFuns;
        this.numberOfParam = numberOfParam;
        this.maxFilterLength = maxFilterLength;
    }

    public int getNumberOfParam() { return this.numberOfParam; }
    public List<Function<List<Value>, Value>> getAggrFuns() {
        return this.aggrFuns;
    }
    public List<ValNode> constValNodes() {
        return this.constValNodes;
    }
    public int maxDepth() { return this.maxDepth; }
    public void setMaxFilterLength(int maxFilterLength) { this.maxFilterLength = maxFilterLength; }
    public int maxFilterLength() { return maxFilterLength; }
    public void setNumberOfParam(int n) {this.numberOfParam = n; }

    private class JsonConstraint {
        int max_depth;
        List<String> constants;
        List<String> aggregation_functions;
        int parameter_number;
        int max_filter_length;

        public JsonConstraint(String fileContent) {
            Gson gson = new GsonBuilder().create();
            JsonConstraint p = gson.fromJson(fileContent, JsonConstraint.class);
            this.max_depth = p.max_depth;
            this.constants = p.constants;
            this.aggregation_functions = p.aggregation_functions;
            this.parameter_number = p.parameter_number;
            this.max_filter_length = p.max_filter_length;
            if (constants == null) constants = new ArrayList<>();
            if (aggregation_functions == null) aggregation_functions = new ArrayList<>();
        }

        public String toString() {
            String s = "maxdepth: " + max_depth + "\n";
            s += "constants: " + constants.stream().reduce(String::concat) + "\n";
            s += "aggr: " + aggregation_functions.stream().reduce(String::concat) + "\n";
            s += "pn: " + parameter_number;
            s += "maxfl: " + max_filter_length;
            return s;
        }
    }

    public Constraint(String fileContent) {
        JsonConstraint p = new JsonConstraint(fileContent);
        this.maxDepth = p.max_depth;
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
                        default: return null;
                    }
                }
        ).filter(x ->  x != null).collect(Collectors.toList());
        this.numberOfParam = p.parameter_number;
        this.maxFilterLength = p.max_filter_length;
    }
}
