package springredis.demo.error;

public class DataBaseObjectNotFoundException extends Exception{
    public DataBaseObjectNotFoundException() {
        super();
    }

    public DataBaseObjectNotFoundException(String message) {
        super(message);
    }
}
