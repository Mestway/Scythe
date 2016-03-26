package invariant.lang.cval;

import javafx.util.Pair;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by clwang on 3/6/16.
 */
public class CLookupVal extends ConstraintVal {

    String target;
    List<Pair<String, String>> matchPairs = new ArrayList<>();

    public CLookupVal(String target, List<Pair<String, String>> matchPairs) {
        this.target = target;
    }

    @Override
    public Value eval(TableRow outputRow, Table inputTable) {

        List<Value> result = new ArrayList<>();

        int targetIndex = inputTable.retrieveIndex(target);
        List<Integer> inputIndex = new ArrayList<>();
        List<Value> outputValue = new ArrayList<>();
        for (Pair<String, String> p : matchPairs) {
            inputIndex.add(inputTable.retrieveIndex(p.getKey()));
            outputValue.add(outputRow.getValue(outputRow.retrieveIndex(p.getValue())));
        }
        for (TableRow tr : inputTable.getContent()) {
            boolean satisfy = true;
            for (int i = 0 ; i < inputIndex.size(); i ++) {
                if (!outputValue.get(i).equals(tr.getValue(inputIndex.get(i)))) {
                    satisfy = false;
                }
            }
            if (satisfy)
                result.add(tr.getValue(targetIndex));
        }

        if (result.size() == 1)
            return result.get(0);
        else {
            System.out.println("[ERROR@CLookup42] Not valid");
            return null;
        }
    }
}
