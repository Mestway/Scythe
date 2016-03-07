package constraint.lang.filter;

import sql.lang.DataType.Value;
import sql.lang.TableRow;

/**
 * Created by clwang on 3/7/16.
 */
public class InParameter extends Parameter {

    String name;

    public InParameter(String name) {
        this.name = name;
    }

    @Override
    public Value instantiate(TableRow outTr, TableRow inTr) {
        return inTr.getValue(inTr.retrieveIndex(name));
    }
}
