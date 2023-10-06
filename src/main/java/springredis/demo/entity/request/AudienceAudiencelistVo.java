package springredis.demo.entity.request;


import lombok.Data;

import java.io.Serializable;

@Data
public class AudienceAudiencelistVo implements Serializable {

    private long audiencelistId;
    private long audienceId;

}
