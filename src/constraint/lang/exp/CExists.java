package constraint.lang.exp;

import constraint.lang.filter.CFilter;
import sql.lang.Table;
import sql.lang.TableRow;

/**
 * Created by clwang on 3/6/16.
 */
public class CExists extends ConstraintExp {

    CFilter filter;

    public CExists(CFilter f) { this.filter = f; }

    @Override
    public boolean eval(TableRow outTr, Table input) {
        return input.getContent().stream()
                .map(inTr -> filter.eval(outTr, inTr)).reduce(false, (x, y)-> (x || y));
    }
}
