package springredis.demo.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    @JsonProperty("status_code")
    private int statusCode;

    @JsonProperty("msg")
    private String msg;
}
