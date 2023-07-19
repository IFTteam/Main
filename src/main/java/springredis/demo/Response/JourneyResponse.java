package springredis.demo.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;



@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Component
public class JourneyResponse {
    private Long journeyId;
    private String journeyName;
    private String createBy;
    private String frontendJourneyId;

}
