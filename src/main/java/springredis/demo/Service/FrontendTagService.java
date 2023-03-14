package springredis.demo.Service;


import springredis.demo.entity.Tag;
import springredis.demo.entity.TagDetail;
import springredis.demo.error.AudienceNotFoundException;
import springredis.demo.error.UserNotFoundException;

import java.util.List;

public interface FrontendTagService {
    List<String> getDistinctTagByUser(Long userId) throws UserNotFoundException;

    Tag saveTagWithUserAndJourney(long userId, Tag tag);

    List<TagDetail> getTagAndJourneyByAudience(Long audienceId) throws AudienceNotFoundException;
}
