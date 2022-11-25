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
public class Audience extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "audience_sequence",
            sequenceName = "audience_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "audience_sequence"
    )
    private long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;

    private LocalDate birthday;
    private String source;

    @ManyToOne(targetEntity = Node.class)
    @JoinColumn(name="audience_node_id",referencedColumnName = "id")
    private Node node;

    @ManyToOne(
            cascade = CascadeType.ALL
    )
    @JoinColumn(
            name = "user_id"
    )
    private User user;

    @ManyToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL
    )
    @JoinTable(
            name = "map",
            joinColumns = @JoinColumn(
                    name = "audience_id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "tag_id"
            )
    )
    @JsonIgnore
    private List<Tag> tags = new ArrayList<>();

    public void addTags(Tag tag){
        if(tags == null) tags = new ArrayList<>();
        tags.add(tag);
    }
    private LocalDate date_added;
    private LocalDate last_updated_time;
}
