package sql.lang.datatype;

/**
 * Created by clwang on 1/4/16.
 */
public enum ValType {
    DateVal ("date"),
    NumberVal ("num"),
    StringVal ("str"),
    TimeVal ("time");

    private final String nameStr;

    ValType(String name) {
        this.nameStr = name;
    }

    public String toString() {
        return this.nameStr;
    }
}
