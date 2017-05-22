package lang.sql.exception;

/**
 * Created by clwang on 1/6/16.
 */
public class SQLEvalException extends Exception {
    String message = "";

    public SQLEvalException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
