package springredis.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.Service.FrontendTagService;
import springredis.demo.entity.Tag;
import springredis.demo.error.AudienceNotFoundException;
import springredis.demo.error.UserNotFoundException;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class FrontendTagController {

    @Autowired
    private FrontendTagService frontendTagService;


    // Task 2, input is user_id, change return to Tag object
    @GetMapping("/tag/{id}")
    public List<String> getDistinctTagByUser(@PathVariable("id") Long userId) throws UserNotFoundException {
        return frontendTagService.getDistinctTagByUser(userId);
    }


    // Task 3 and 4, create/select tag, input user_id, journey_id, tag_name, no output
    @PostMapping("/tags/{user}/{journey}")
    public Tag saveTagWithUserAndJourney(@PathVariable("user")long userId, @PathVariable("journey")long journeyId, @RequestBody Tag tag) {
        return frontendTagService.saveTagWithUserAndJourney(userId, journeyId, tag);
    }


    // Task 5, input is audience_id, change to return Tag Object
    @GetMapping("/tags/audiences/{id}")
    public List<Tag> getTagAndJourneyByAudience(@PathVariable("id") Long audienceId) throws AudienceNotFoundException {
        return frontendTagService.getTagAndJourneyByAudience(audienceId);
    }
}
