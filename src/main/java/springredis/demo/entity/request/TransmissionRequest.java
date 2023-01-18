package springredis.demo.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransmissionRequest {
    @JsonProperty("campaign_id")
    private String campaignId;

    @JsonProperty("recipients")
    private List<Address> addressList;

    @JsonProperty("content")
    private Content content;

    @JsonProperty("audience_id")
    private Long audienceId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("journey_id")
    private Long journeyId;

}
