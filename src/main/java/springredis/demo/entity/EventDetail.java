package springredis.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDetail {
    @JsonProperty("transmission_id")
    private String transmissionId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("rcpt_to")
    private String audienceEmail;
}
