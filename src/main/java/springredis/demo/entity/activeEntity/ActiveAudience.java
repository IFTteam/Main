package springredis.demo.entity.activeEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="active_audience")
@Data

public class ActiveAudience implements Serializable {
    @Id
    @GeneratedValue
    @JoinColumn(name="ID",referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Long id;

    @Column(name="AudienceId")
    private Long AudienceId;

    @ManyToOne
    @JoinColumn(name="audience_node_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
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