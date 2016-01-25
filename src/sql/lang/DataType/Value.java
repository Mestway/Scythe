package sql.lang.DataType;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by clwang on 12/12/15.
 * The type Value represent
 */
public interface Value {

    String getRaw();
    Object getVal();
    boolean equals(Value v);
    Value duplicate();
    ValType getValType();

    static Value parse(String raw) {
        try {
            // parse as a float
            Double doubleVal = Double.parseDouble(raw);
            NumberVal val = new NumberVal(doubleVal);
            val.setRaw(raw);
            return val;
        } catch (Exception e) {}

        try {
            // try to parse the value as a date-time value
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            DateVal val = new DateVal(dateFormat.parse(raw));
            val.setRaw(raw);
            return val;
        } catch (Exception e) {}
        try {
            // try to parse the value as a date-time value
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            DateVal val = new DateVal(dateFormat.parse(raw));
            val.setRaw(raw);
            return val;
        } catch (Exception e) {}
        try {
            // try to parse the value as a date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            DateVal val = new DateVal(dateFormat.parse(raw));
            val.setRaw(raw);
            return val;
        } catch (Exception e) {}
        try {
            // try to parse the value as a date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            DateVal val = new DateVal(dateFormat.parse(raw));
            val.setRaw(raw);
            return val;
        } catch (Exception e) {}
        try {
            // parse time
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date date = sdf.parse(raw);
            TimeVal val = new TimeVal(new Time(date.getTime()));
            val.setRaw(raw);
            return val;
        } catch (Exception e) {}

        try {
            // parse time
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            Date date = sdf.parse(raw);
            TimeVal val = new TimeVal(new Time(date.getTime()));
            val.setRaw(raw);
            return val;
        } catch (Exception e) {}

        return new StringVal(raw);
    }
}
