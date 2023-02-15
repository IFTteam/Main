package springredis.demo.error;

public class TimeTaskNotExistException extends Exception{
    public TimeTaskNotExistException() {
        super();
    }

    public TimeTaskNotExistException(String message) {
        super(message);
    }

    public TimeTaskNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public TimeTaskNotExistException(Throwable cause) {
        super(cause);
    }

    protected TimeTaskNotExistException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
