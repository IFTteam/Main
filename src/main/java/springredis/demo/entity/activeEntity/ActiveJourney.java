package springredis.demo.entity.activeEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="active_journey")
@NoArgsConstructor
public class ActiveJourney {
    @Id
    @GeneratedValue

    private Long id;
    private Long journeyId;

    @OneToMany(mappedBy = "activeJourney")
    private List<ActiveNode> activeNodeList = new ArrayList<>();




//    @OneToMany(mappedBy = "activeJourney")
//    private List<ActiveAudience> activeAudienceList = new ArrayList<>();

    public ActiveJourney(Long journeyId) {
        this.journeyId = journeyId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJourneyId() {
        return journeyId;
    }

    public void setJourneyId(Long journeyId) {
        this.journeyId = journeyId;
    }

    public List<ActiveNode> getActiveNodeList() {
        return activeNodeList;
    }

    public void setActiveNodeList(List<ActiveNode> activeNodeList) {
        this.activeNodeList = activeNodeList;
    }
}