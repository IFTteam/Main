package springredis.demo.error;


public class DataBaseObjectNotFoundException extends RuntimeException {
    public DataBaseObjectNotFoundException() {
        super();
    }

    public DataBaseObjectNotFoundException(String message) {
        super(message);
    }

    public DataBaseObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataBaseObjectNotFoundException(Throwable cause) {
        super(cause);
    }

    public DataBaseObjectNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
