package springredis.demo.entity.activeEntity;
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

    @OneToMany(mappedBy = "activeJourney", cascade = CascadeType.REMOVE)
    private List<ActiveNode> activeNodeList = new ArrayList<>();
  
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
}