package springredis.demo.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SparkPostResults {
    @JsonProperty("total_rejected_recipients")
    private String totalRejectedRecipients;
    @JsonProperty("total_accepted_recipients")
    private String totalAcceptedRecipients;
    @JsonProperty("id")
    private Long transmissionId;
}
