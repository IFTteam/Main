package springredis.demo.entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AudienceList extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "audiencelist_sequence",
            sequenceName = "audiencelist_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE
    )
    private long id;
    private String audienceListName;

    @ManyToMany(cascade = { CascadeType.ALL })
    @JoinTable(
            name = "audience_audiencelist",
            joinColumns = { @JoinColumn(name = "audiencelist_id") },
            inverseJoinColumns = { @JoinColumn(name = "audience_id") }
    )
    List<Audience> audiences = new ArrayList<>();

    public void removeAudience(Audience audience) {
        this.audiences.remove(audience);
        audience.getAudienceLists().remove(this);
    }

    @ManyToOne(
            cascade = CascadeType.ALL
    )
    @JoinColumn(
            name = "user_id"
    )
    private User user;



}

