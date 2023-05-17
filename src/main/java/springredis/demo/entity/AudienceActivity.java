package springredis.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AudienceActivity {

    @Id
    @SequenceGenerator(
            name = "audience_activity_sequence",
            sequenceName = "audience_activity_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "audience_activity_sequence"
    )

    //@Column(name = "audience_activity_id")
    private long AudienceActivityId;

    private String eventType;

    @Column(name = "audience_email")
    private String audience_email;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "audience_id")
    private Audience audience;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @JsonIgnore
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    @JsonIgnore
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", insertable = false)
    @JsonIgnore
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", insertable = false)
    @JsonIgnore
    private String updatedBy;

    @Column(name = "transmission_id")
    private Long transmission_id;
}