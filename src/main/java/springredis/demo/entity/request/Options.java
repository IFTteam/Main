package springredis.demo.entity.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Options {
    @JsonProperty("start_time")
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime startTime;

    @JsonProperty("open_tracking")
    private boolean openTracking;

    @JsonProperty("click_tracking")
    private boolean clickTracking;

    @Override
    public String toString() {
        return "Options{" +
                "startTime='" + startTime + '\'' +
                ", open_tracking='" + openTracking + '\'' +
                ", click_tracking='" + clickTracking + '\'' +
                '}';
    }

}