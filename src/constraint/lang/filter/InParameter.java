package constraint.lang.filter;

import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.TableRow;
import util.IndentionManagement;

import java.util.Objects;

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

    @Override
    public ValType getType(Table inT, Table outT) {
        return checkType(inT);
    }

    @Override
    public String toString() {
        return name;
    }

    public ValType checkType(Table input) {
        return input.getSchemaType().get(input.retrieveIndex(name));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InParameter)
            return ((InParameter) obj).name.equals(this.name);
        return false;
    }

}
