package springredis.demo.Service;

import springredis.demo.entity.Audience;
import springredis.demo.entity.AudienceList;
import springredis.demo.entity.request.AudienceAudiencelistVo;

public interface AudienceListService {

    AudienceList save(AudienceList audienceList);

    Audience addAudienceToAudienceList(AudienceAudiencelistVo audienceAudiencelistVo);

}
