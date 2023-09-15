package springredis.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.io.Serializable;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@IdClass(AudienceAudiencelistId.class)
public class AudienceAudiencelist implements Serializable {

    @Id
    @Column(name = "audiencelist_id")
    private long audiencelistId;
    @Id
    @Column(name = "audience_id")
    private long audienceId;

}
