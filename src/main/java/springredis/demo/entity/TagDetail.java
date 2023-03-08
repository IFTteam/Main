package springredis.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagDetail {
    @Id
    @GeneratedValue(
            strategy = GenerationType.AUTO
    )
    private long id;

    private long tagId;

    @ManyToOne(
            cascade = CascadeType.ALL
    )
    @JoinColumn(
            name = "journey_id"
    )
    private Journey journey;

    @ManyToMany(mappedBy = "tagDetails", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Audience> audiences = new ArrayList<>();
}
