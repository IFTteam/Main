package springredis.demo.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class Msys {
    @JsonProperty("message_event")
    @JsonAlias({"track_event", "gen_event", "unsubscribe_event", "relay_event"})
    private EventDetail eventDetail;
}
