package sql.lang.datatype;

/**
 * Created by clwang on 12/14/15.
 */
public class NumberVal implements Value {
    Double val;

    public NumberVal(Double val) {
        this.val = val;
    }

    public NumberVal(Integer val) {
        this.val = val.doubleValue();
    }

    @Override
    public Double getVal() { return this.val; }
    @Override
    public boolean equals(Value v) {
        return this.val.equals(v.getVal());
    }
    @Override
    public String toString() { return this.val.toString(); }
    @Override
    public NumberVal duplicate() {
        return new NumberVal(this.val);
    }
    @Override
    public ValType getValType() {
        return ValType.NumberVal;
    }
}
