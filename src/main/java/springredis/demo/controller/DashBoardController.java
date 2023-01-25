package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.entity.Audience;
import springredis.demo.entity.AudienceList;
import springredis.demo.repository.AudienceListRepository;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.entity.response.AudienceListResponse;

import java.util.List;

@RestController
public class DashBoardController {
    @Autowired
    AudienceRepository audienceRepository;

    @Autowired
    AudienceListRepository audienceListRepository;


    @PostMapping("dashboard/CreateAudienceList")
    public AudienceList createAudienceList(@RequestBody AudienceListResponse response){
        AudienceList audienceList = new AudienceList();
        audienceList.setAudienceListName(response.getAudienceListName());
        audienceListRepository.save(audienceList);
        return audienceList;
    }

    @PostMapping("dashboard/UpdateRelation/{audienceListId}")
    public AudienceList updateAudienceListRelation(@PathVariable Long audienceListId, @RequestBody AudienceListResponse response){
        AudienceList audienceList = audienceListRepository.findById(audienceListId).get();
        for(Long id: response.getAudienceId()){
            Audience audience = audienceRepository.findById(id).get();
            audience.getAudienceLists().add(audienceList);
            audienceRepository.save(audience);
            audienceList.getAudiences().add(audience);
        }
        audienceListRepository.save(audienceList);
        return audienceList;
    }


    @GetMapping("dashboard/getAllAudienelist")
    public List<AudienceList> getAllAudienceList(){
        return audienceListRepository.findAll();
    }
}
