package sql.lang.datatype;

import java.sql.Time;

/**
 * Created by clwang on 12/14/15.
 */
public class TimeVal implements Value {

    Time val;

    public TimeVal(Time time) {
        this.val = time;
    }

    @Override
    public Time getVal() { return this.val; }

    @Override
    public int hashCode() {
        return this.getVal().hashCode();
    }

    @Override
    public boolean equals(Object v) {
        if (v instanceof TimeVal) {
            return this.val.equals(((TimeVal) v).getVal());
        }
        return false;
    }

    @Override
    public String toString() {
        return val.toString();
    }

    public TimeVal duplicate() {
        return new TimeVal(this.val);
    }

    @Override
    public ValType getValType() {
        return ValType.TimeVal;
    }

}
