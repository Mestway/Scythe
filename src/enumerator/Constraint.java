package enumerator;

import sql.lang.DataType.Value;
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
    Integer maxDepth = 2;
    List<ValNode> constValNodes = new ArrayList<>();

    List<Function<List<Value>, Value>> aggrFuns = new ArrayList<>();

    public Constraint(Integer maxDepth,
                      List<Value> constants,
                      List<Function<List<Value>, Value>> aggrFuns) {
        this.maxDepth = maxDepth;
        this.constValNodes = constants.stream()
                .map(c -> new ConstantVal(c)).collect(Collectors.toList());
        this.aggrFuns = aggrFuns;
    }

    public List<Function<List<Value>, Value>> getAggrFuns() {
        return this.aggrFuns;
    }

    public List<ValNode> constValNodes() {
        return this.constValNodes;
    }
}
