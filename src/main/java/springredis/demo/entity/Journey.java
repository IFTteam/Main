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

    /**
     * journey current status is not activate yet
     */
    public static final Integer NOT_ACTIVATE = 0;

    /**
     * journey current status is activating
     */
    public static final Integer ACTIVATING = 1;

    /**
     * journey current status already activated but paused
     */
    public static final Integer ACTIVATED_PAUSED = 2;

    /**
     * journey current status already activated and is running
     */
    public static final Integer ACTIVATED_RUNNING = 3;

    /**
     * journey current status already activated and finished
     */
    public static final Integer ACTIVATED_FINISHED = 4;

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
