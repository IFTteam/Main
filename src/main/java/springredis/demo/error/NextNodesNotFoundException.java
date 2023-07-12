package springredis.demo.error;

public class NextNodesNotFoundException extends RuntimeException {
    public NextNodesNotFoundException(String msg) {
        super(msg);
    }
}
