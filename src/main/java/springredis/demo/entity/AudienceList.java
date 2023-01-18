package springredis.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.*;
import java.sql.Date;
import java.time.LocalDate;
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

}

