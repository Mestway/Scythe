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
    public boolean equals(Value v) {
        return this.val.equals(v.getVal());
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
