package springredis.demo.entity.response;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.error.JourneyNotFoundException;

@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(JourneyNotFoundException.class)
    public Response exceptionHandler(JourneyNotFoundException ex) {
        String msg = ex.getMessage();
        log.info(msg);
        return new Response(HttpStatus.SC_INTERNAL_SERVER_ERROR, msg);
    }
}
