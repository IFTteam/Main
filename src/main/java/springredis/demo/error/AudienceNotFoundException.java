package springredis.demo.error;

public class AudienceNotFoundException extends Exception{

    public AudienceNotFoundException() {
        super();
    }

    public AudienceNotFoundException(String message) {
        super(message);
    }

    public AudienceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public AudienceNotFoundException(Throwable cause) {
        super(cause);
    }

    protected AudienceNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
