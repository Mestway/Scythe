package sql.lang.DataType;

/**
 * Created by clwang on 12/14/15.
 */
public class FloatVal implements Value {
    String raw;
    Float val;

    public FloatVal(Float val) {
        this.val = val;
        this.raw = val.toString();
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public Float getVal() { return this.val; }

    @Override
    public boolean equals(Value v) {
        return this.val.equals(v.getVal());
    }

    public String getRaw() { return this.raw; }

    public String toString() { return this.val.toString(); }

    public FloatVal duplicate() {
        return new FloatVal(this.val);
    }
}
