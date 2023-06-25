package springredis.demo.error;

public class DataBaseObjectNotFoundException extends RuntimeException {
    public DataBaseObjectNotFoundException() {
        super();
    }

    public DataBaseObjectNotFoundException(String message) {
        super(message);
    }
}
