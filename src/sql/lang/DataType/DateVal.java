package sql.lang.DataType;

import java.util.Date;

/**
 * Created by clwang on 12/14/15.
 */
public class DateVal implements Value {
    String raw;

    Date val;

    public DateVal(Date date) {
        this.val = date;
    }

    private DateVal() {};

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getRaw() { return this.raw; }
    public Date getVal() { return this.val; }

    @Override
    public boolean equals(Value v) {
        return this.val.equals(v.getVal());
    }

    @Override
    public Value duplicate() {
        DateVal newVal = new DateVal();
        newVal.raw = this.raw;
        newVal.val = (Date) this.val.clone();
        return newVal;
    }

    public String toString() { return this.getVal().toString(); }

}
