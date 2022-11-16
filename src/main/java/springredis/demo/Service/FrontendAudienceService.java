package springredis.demo.Service;


import springredis.demo.entity.Audience;
import springredis.demo.error.AudienceNotFoundException;
import springredis.demo.error.TagNotFoundException;
import springredis.demo.error.UserNotFoundException;

import java.util.List;

public interface FrontendAudienceService {
    List<Audience> getAudienceAndTagByUser(Long userId) throws UserNotFoundException;

    Audience createRelationBetweenAudienceAndTag(Long audienceId, Long tagId) throws AudienceNotFoundException, TagNotFoundException;
}
