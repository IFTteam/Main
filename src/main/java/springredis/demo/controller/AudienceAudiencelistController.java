package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.Service.AudienceAudiencelistService;
import springredis.demo.entity.AudienceAudiencelist;

@RestController
@RequestMapping("/audienceAudiencelist")
public class AudienceAudiencelistController {

    @Autowired
    private AudienceAudiencelistService audienceAudiencelistService;

    @PostMapping("/add")
    public ResponseEntity<Object> add(@RequestBody AudienceAudiencelist audienceAudiencelist) {
        AudienceAudiencelist audienceAudiencelistResult = audienceAudiencelistService.save(audienceAudiencelist);
        return new ResponseEntity<>(audienceAudiencelistResult, HttpStatus.CREATED);
    }

}
