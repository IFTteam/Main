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
            (shape = JsonFormat.Shape.STRING, pattern = "YYYY-MM-DDTHH:MM:SS+-HH:MM")
    private LocalDateTime startTime;

    @JsonProperty("open_tracking")
    private String openTracking;

    @JsonProperty("click_tracking")
    private String clickTracking;

}
