package springredis.demo.entity;

import lombok.Data;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.*;

@Entity
@Data
@Table
public class Journey extends BaseEntity {
    @Id
    @GeneratedValue

    private Long id;
    private String journeyName;
    private String thumbnailUrl;
    private String journeySerialized;
    private Integer status;
    private String stage;
    private String frontEndId;
}
