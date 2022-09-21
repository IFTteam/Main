package springredis.demo.entity.activeEntity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table
@Data
public class ActiveAudience {
    @Id
    @GeneratedValue

    private Long id;
    private Long AudienceId;
    @ManyToOne(targetEntity = ActiveNode.class)
    @JoinColumn(name="audience_node_id",referencedColumnName = "id")
    private ActiveNode activeNode;
    public ActiveAudience() {
    }
    public ActiveAudience(Long audienceId) {
        AudienceId = audienceId;
    }

}