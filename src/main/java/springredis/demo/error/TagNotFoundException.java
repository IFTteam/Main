package springredis.demo.error;

public class TagNotFoundException extends Exception{
    public TagNotFoundException() {
        super();
    }

    public TagNotFoundException(String message) {
        super(message);
    }

    public TagNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public TagNotFoundException(Throwable cause) {
        super(cause);
    }

    protected TagNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
