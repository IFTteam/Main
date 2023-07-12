package springredis.demo.error;

public class WorldCityNotExistException extends RuntimeException {
    public WorldCityNotExistException(String msg) {
        super(msg);
    }
}
