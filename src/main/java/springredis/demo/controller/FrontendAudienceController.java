package springredis.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.Service.FrontendAudienceService;
import springredis.demo.entity.Audience;
import springredis.demo.error.UserNotFoundException;

import java.util.List;

@RestController
public class FrontendAudienceController {

    @Autowired
    private FrontendAudienceService frontendAudienceService;

    // Task 1, input is user_id, change return to Audience object
    @GetMapping("/audience/{id}")
    public List<Audience> getAudienceAndTagByUser(@PathVariable("id") Long userId) throws UserNotFoundException {
        return frontendAudienceService.getAudienceAndTagByUser(userId);
    }
}
