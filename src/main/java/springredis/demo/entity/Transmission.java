package springredis.demo.entity;

import lombok.Getter;
import lombok.Setter;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name="transmission")
public class Transmission extends BaseEntity {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "audience_email")
    private String audience_email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audience_id",  nullable = false)
    private Audience audience;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id",  nullable = false)
    private Journey journey;
}
