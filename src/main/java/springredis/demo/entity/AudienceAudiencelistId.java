package springredis.demo.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class AudienceAudiencelistId implements Serializable {

    private long audiencelistId;
    private long audienceId;

}
