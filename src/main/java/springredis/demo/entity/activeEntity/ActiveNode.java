package springredis.demo.entity.activeEntity;

import lombok.Data;
import lombok.NoArgsConstructor;
import springredis.demo.entity.Journey;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="active_node")
@NoArgsConstructor
public class ActiveNode {
    @Id
    @GeneratedValue
    private Long id;
    private Long nodeId;


    @ManyToOne(targetEntity = ActiveJourney.class)
    @JoinColumn(name="node_journey_id",referencedColumnName = "id")
    private ActiveJourney activeJourney;

    @OneToMany(mappedBy = "activeNode")
    private List<ActiveAudience> activeAudienceList = new ArrayList<>();
    public ActiveNode(Long nodeId, ActiveJourney activeJourney) {
        this.nodeId = nodeId;
        this.activeJourney = activeJourney;
    }

    public List<ActiveAudience> getActiveAudienceList() {
        return activeAudienceList;
    }

    public void setActiveAudienceList(List<ActiveAudience> activeAudienceList) {
        this.activeAudienceList = activeAudienceList;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public ActiveJourney getActiveJourney() {
        return activeJourney;
    }

    public void setActiveJourney(ActiveJourney activeJourney) {
        this.activeJourney = activeJourney;
    }
}
