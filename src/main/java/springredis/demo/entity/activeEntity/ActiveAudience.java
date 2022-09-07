package springredis.demo.entity.activeEntity;

import lombok.Data;

import javax.persistence.*;

@Entity
public class ActiveAudience {
    @Id
    @GeneratedValue
    private Long id;
    private Long AudienceId;

    @ManyToOne(targetEntity = ActiveNode.class)
    @JoinColumn(name="audience_node_id",referencedColumnName = "id")
    private ActiveNode activeNode;

//    @ManyToOne(targetEntity = ActiveJourney.class)
//    @JoinColumn(name="node_journey_id",referencedColumnName = "id")
//    private ActiveNode activeNode;

    public ActiveAudience() {
    }

    public ActiveAudience(Long audienceId) {
        AudienceId = audienceId;
    }

    public ActiveNode getActiveNode() {
        return activeNode;
    }

    public void setActiveNode(ActiveNode activeNode) {
        this.activeNode = activeNode;
    }

    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public Long getAudienceId() {
        return AudienceId;
    }

    public void setAudienceId(Long audienceId) {
        AudienceId = audienceId;
    }
}
