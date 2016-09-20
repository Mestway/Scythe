package sql.lang.datatype;

/**
 * Created by clwang on 12/14/15.
 */
public class StringVal implements Value {
    String val;

    public StringVal(String str) {
        this.val = str.trim();
    }

    @Override
    public String getVal() { return this.val; }
    @Override
    public int hashCode() {
        return this.getVal().hashCode();
    }
    @Override
    public boolean equals(Object v) {
        if (v instanceof StringVal)
            return this.val.equals(((StringVal) v).getVal());
        else
            return false;
    }
    @Override
    public String toString() {
        return val;
    }
    @Override
    public StringVal duplicate() {
        return new StringVal(this.val);
    }
    @Override
    public ValType getValType() {
        return ValType.StringVal;
    }
}
