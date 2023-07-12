package springredis.demo.utils;

import springredis.demo.error.DataBaseObjectNotFoundException;

import java.util.Optional;

public class OptionalUtils {
    public static <T> T getObjectOrThrow(Optional<T> optional, String errorMessage) {
        if (optional.isEmpty()) {
            throw new DataBaseObjectNotFoundException(errorMessage);
        }
        return optional.get();
    }
}
