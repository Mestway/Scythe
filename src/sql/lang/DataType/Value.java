package sql.lang.datatype;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by clwang on 12/12/15.
 * The type Value represent
 */
public interface Value {

    Object getVal();
    Value duplicate();
    ValType getValType();

    static Value parse(String raw) {
        try {
            // parse as a float
            Double doubleVal = Double.parseDouble(raw);
            NumberVal val = new NumberVal(doubleVal);
            return val;
        } catch (Exception e) {}

        try {
            // try to parse the value as a date-time value
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            DateVal val = new DateVal(dateFormat.parse(raw));
            return val;
        } catch (Exception e) {}
        try {
            // try to parse the value as a date-time value
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            DateVal val = new DateVal(dateFormat.parse(raw));
            return val;
        } catch (Exception e) {}
        try {
            // try to parse the value as a date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            DateVal val = new DateVal(dateFormat.parse(raw));
            return val;
        } catch (Exception e) {}
        try {
            // try to parse the value as a date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            DateVal val = new DateVal(dateFormat.parse(raw));
            return val;
        } catch (Exception e) {}
        try {
            // parse time
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date date = sdf.parse(raw);
            TimeVal val = new TimeVal(new Time(date.getTime()));
            return val;
        } catch (Exception e) {}

        try {
            // parse time
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            Date date = sdf.parse(raw);
            TimeVal val = new TimeVal(new Time(date.getTime()));
            return val;
        } catch (Exception e) {}

        if (raw.startsWith("NULL[")) {
            ValType ty = parseValType(raw.substring(raw.indexOf("[") + 1, raw.indexOf("]")));
            return new NullVal(ty);
        }

        return new StringVal(raw);
    }

    static ValType parseValType(String typeRawString) {

        if (typeRawString.equals("date")) {
            return ValType.DateVal;
        } else if (typeRawString.equals("num")) {
            return ValType.NumberVal;
        } else if (typeRawString.equals("str")) {
            return ValType.StringVal;
        } else if (typeRawString.equals("time")) {
            return ValType.TimeVal;
        }

        return ValType.StringVal;
    }
}
