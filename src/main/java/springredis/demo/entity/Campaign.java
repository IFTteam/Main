package springredis.demo.entity;

import lombok.Data;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.sql.Date;

@Data
@Entity
public class Campaign extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String emailTemplateSerialized;
    private String sender;
    private String subject;
    private String campaignIndustry;
    private String replyTo;
    private Integer runTimes;
    private Date sendTime;
    private Integer numberOfRecipients;
    private Integer numberDelivered;
    private Integer numberOpened;
    private Float percentageOpened;
    private Integer numberClicked;
    private Float percentageClicked;
    private Integer numberUnsubscribed;
    private Float percentageUnsubscribed;
}
