package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.Service.AudienceListService;
import springredis.demo.entity.Audience;
import springredis.demo.entity.AudienceList;
import springredis.demo.entity.request.AudienceAudiencelistVo;

@RestController
@RequestMapping("/audiencelist")
public class AudiencelistController {

    @Autowired
    private AudienceListService audienceListService;

    @PostMapping("/add")
    public ResponseEntity<Object> add(@RequestBody AudienceList audienceList) {
        AudienceList audienceListResult = audienceListService.save(audienceList);
        return new ResponseEntity<>(audienceListResult, HttpStatus.CREATED);
    }

    @PostMapping("/audienceAudiencelist/add")
    public ResponseEntity<Object> add(@RequestBody AudienceAudiencelistVo audienceAudiencelistVo) {
        Audience saveResult = audienceListService.addAudienceToAudienceList(audienceAudiencelistVo);
        return new ResponseEntity<>(saveResult, HttpStatus.CREATED);
    }

}
