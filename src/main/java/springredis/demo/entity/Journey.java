package springredis.demo.entity;

import lombok.Data;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.*;

import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Entity
@Data
@Table
@Validated
public class Journey extends BaseEntity {
    @Id
    @GeneratedValue
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
}
