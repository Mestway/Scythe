package invariant.lang.cval;

import invariant.lang.filter.CFilter;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/6/16.
 */
public class CAggrVal extends ConstraintVal {

    Function<List<Value>, Value> aggrfun;
    String target;
    CFilter filter;

    public CAggrVal(Function<List<Value>, Value> function,
                    String target,
                    CFilter filter) {
        this.target = target;
        this.filter = filter;
        this.aggrfun = function;
    }

    @Override
    public Value eval(TableRow outTr, Table inputTable) {
        Integer targetIndex = inputTable.retrieveIndex(target);
        List<Value> vals = inputTable.getContent()
                .stream()
                .filter(tr -> filter.eval(outTr, tr))
                .map(tr -> tr.getValue(targetIndex))
                .collect(Collectors.toList());
        return aggrfun.apply(vals);
    }
}
