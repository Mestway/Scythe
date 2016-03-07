package constraint.lang.filter;

import sql.lang.DataType.Value;
import sql.lang.TableRow;

/**
 * Created by clwang on 3/7/16.
 */
public class OutParameter extends Parameter {

    String name;

    public OutParameter(String name) {
        this.name = name;
    }

    @Override
    public Value instantiate(TableRow outTr, TableRow inTr) {
        return outTr.getValue(outTr.retrieveIndex(name));
    }
}
