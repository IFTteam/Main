package springredis.demo.entity;

import lombok.Data;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table
public class Journey extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "journey_sequence",
            sequenceName = "journey_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "journey_sequence"
    )
    private Long id;
    private String journeyName;
    private String thumbnailUrl;
    @Lob
    private String journeySerialized;
    private Integer status;
    private String stage;
    private String frontEndId;

    public Journey() { super(); }

    public Journey (String journeyName, String thumbnailUrl, String journeySerialized, Integer status, String stage, String frontEndId, LocalDateTime createdAt, String createdBy, LocalDateTime updatedAt, String updatedBy) {
        super(createdAt, createdBy, updatedAt, updatedBy);
        this.journeyName = journeyName;
        this.thumbnailUrl = thumbnailUrl;
        this.journeySerialized = journeySerialized;
        this.status = status;
        this.stage = stage;
        this.frontEndId = frontEndId;
    }

    @OneToMany(mappedBy = "journey")
    private List<Tag> sendermessage = new ArrayList<>();
}
