package sql.lang.DataType;

/**
 * Created by clwang on 12/14/15.
 */
public class NumberVal implements Value {
    String raw;
    Double val;

    public NumberVal(Double val) {
        this.val = val;
        this.raw = val.toString();
    }

    public NumberVal(Integer val) {
        this.val = val.doubleValue();
        this.raw = val.toString();
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public Double getVal() { return this.val; }

    @Override
    public boolean equals(Value v) {
        return this.val.equals(v.getVal());
    }

    public String getRaw() { return this.raw; }

    public String toString() { return this.val.toString(); }

    public NumberVal duplicate() {
        return new NumberVal(this.val);
    }

    @Override
    public ValType getValType() {
        return ValType.NumberVal;
    }
}
