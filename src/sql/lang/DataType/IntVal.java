package sql.lang.DataType;

/**
 * Created by clwang on 12/14/15.
 */
public class IntVal implements Value {
    String raw;
    Integer val;
    public IntVal(Integer val) {
        this.val = val;
        this.raw = val.toString();
    }
    public void setRaw(String raw) { this.raw = raw; }
    public Integer getVal() { return this.val; }

    @Override
    public boolean equals(Value v) {
        return this.val.equals(v.getVal());
    }

    public String getRaw() { return this.raw; }

    public String toString() { return this.val.toString(); }

    public IntVal duplicate() {
        return new IntVal(this.val);
    }
}
