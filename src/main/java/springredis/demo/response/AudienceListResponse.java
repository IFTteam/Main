package springredis.demo.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AudienceListResponse {
    private String audienceListName;
    private List<Long> audienceId;
}
