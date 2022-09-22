package springredis.demo.entity.activeEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="active_audience")
@Data

public class ActiveAudience implements Serializable {
    @Id
    @GeneratedValue
    private Long id;

    private Long AudienceId;

    @ManyToOne
    @JoinColumn(name="audience_node_id", nullable=false)
    @JsonIgnore
    private ActiveNode activeNode;

    public ActiveAudience() {
    }

    public ActiveAudience(ActiveNode activeNode) {
        this.activeNode = activeNode;
    }

    public ActiveAudience(Long audienceId){
        this.AudienceId =audienceId;
    }

    public ActiveAudience(Long audienceId,ActiveNode activeNode) {
        AudienceId = audienceId;
        this.activeNode =activeNode;
    }

}