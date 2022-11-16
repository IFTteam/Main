package springredis.demo.Service;


import springredis.demo.entity.Tag;
import springredis.demo.error.AudienceNotFoundException;
import springredis.demo.error.UserNotFoundException;

import java.util.List;

public interface FrontendTagService {
    List<String> getDistinctTagByUser(Long userId) throws UserNotFoundException;

    Tag saveTagWithUserAndJourney(long userId, long journeyId, Tag tag);

    List<Tag> getTagAndJourneyByAudience(Long audienceId) throws AudienceNotFoundException;
}
