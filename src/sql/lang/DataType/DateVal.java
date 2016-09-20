package sql.lang.datatype;

import java.util.Date;

/**
 * Created by clwang on 12/14/15.
 */
public class DateVal implements Value {
    Date val;

    public DateVal(Date date) {
        this.val = date;
    }

    private DateVal() {};

    @Override
    public Date getVal() { return this.val; }

    @Override
    public int hashCode() {
        return this.getVal().hashCode();
    }

    @Override
    public boolean equals(Object v) {
        if(v instanceof DateVal)
            return this.getVal().equals(((DateVal) v).getVal());
        else
            return false;
    }

    @Override
    public Value duplicate() {
        DateVal newVal = new DateVal();
        newVal.val = this.val;
        //newVal.val = (Date) this.val.clone();
        return newVal;
    }

    @Override
    public ValType getValType() {
        return ValType.DateVal;
    }

    public String toString() {
        //DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        //return df.format(val);
        return val.toString();
    }

}
