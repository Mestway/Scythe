package invariant.lang.exp;

import javafx.util.Pair;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluating whether columns in the output
 *  table are from values in the input table
 * Created by clwang on 3/6/16.
 */
public class CMapsTo extends ConstraintExp {
    List<String> outColumns;
    List<String> inColumns;

    public CMapsTo(List<String> out, List<String> in) {
        this.outColumns = out;
        this.inColumns = in;
    }

    @Override
    public boolean eval(TableRow outTr, Table input) {

        List<Value> outputVals = new ArrayList<>();
        for (String oc : outColumns) {
            outputVals.add(outTr.getValue(outTr.retrieveIndex(oc)));
        }

        List<Integer> inputIndices = inColumns.stream()
                .map(ic -> input.retrieveIndex(ic)).collect(Collectors.toList());

        return input.getContent().stream()
                .map(tr -> {
                    List<Value> inTRVals = tr.retrieveValuesByIndices(inputIndices);
                    for (int i = 0; i < inTRVals.size(); i ++) {
                        if (! inTRVals.get(i).equals(outputVals.get(i))) {
                            return false;
                        }
                    }
                    return true;
                }).reduce(false, (x,y) -> (x || y));
    }
}
