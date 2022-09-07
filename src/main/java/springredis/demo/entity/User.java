package springredis.demo.entity;

import lombok.Data;
import org.springframework.data.relational.core.sql.In;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Data
@Table
public class User extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String username;
    private String password_hash;
    private String avatarUrl;
    private String domain;
    private Long companyId;
    private String unsubscribeLink;
    private String subscriptionType;
    private String unsubscribeType;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private Long apiId;
    private Long apiKey;
//    private Integer preferEmailSvcProvider;
//    private Integer onlySendDeliverableEmail;

    private String salesforceApiKey;
    private String hubspotApiKey;
    private String shopifyApiKey;
    private String facebookAdsApiKey;
    private String shopifydevstore;                 //shopify development store name for this user

    public User() {
        super();
    }
}
