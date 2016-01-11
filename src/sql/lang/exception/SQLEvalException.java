package sql.lang.exception;

/**
 * Created by clwang on 1/6/16.
 */
public class SQLEvalException extends Exception {
    String message = "";
    ExceptionType type;

    enum ExceptionType {
        TableToValFailure,
        UninstantedHole
    }

    public SQLEvalException(String message) {
        this.message = message;
    }

    public SQLEvalException(ExceptionType type) {
        this.type = type;
    }

    public String getMessage() {
        return this.message;
    }
}
