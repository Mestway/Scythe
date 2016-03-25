package constraint.lang.filter;

import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
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

    @Override
    public ValType getType(Table inT, Table outT) {
        return checkType(outT);
    }

    @Override
    public String toString() {
        return name;
    }

    private ValType checkType(Table output) {
        return output.getSchemaType().get(output.retrieveIndex(name));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OutParameter)
            return ((OutParameter) obj).name.equals(this.name);
        return false;
    }
}
