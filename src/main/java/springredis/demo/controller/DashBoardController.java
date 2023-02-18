package springredis.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.Response.JourneyResponse;
import springredis.demo.Service.DashBoardService;
import springredis.demo.entity.Audience;
import springredis.demo.entity.AudienceList;
import springredis.demo.entity.Journey;
import springredis.demo.entity.response.AudienceListResponse;
import springredis.demo.repository.AudienceListRepository;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.JourneyRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashBoardController {

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private JourneyResponse journeyResponse;

    @Autowired
    private DashBoardService dashBoardService;

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


    @GetMapping("/journey/all/{userid}")
    public List<List<String>> getAllJourneys(@PathVariable("userid") String id){
        return dashBoardService.findAllJourneyById(id);
    }

    @GetMapping("/journey/all")
    public List<JourneyResponse> getJourneys(){
        List<Journey> journeys = journeyRepository.findAll();
        List<JourneyResponse> responses=new ArrayList<>();
        for(Journey journey: journeys) {
            String frontendLink = "http://localhost:3001/"+journey.getFrontEndId();
            JourneyResponse response = new JourneyResponse(journey.getId(), journey.getJourneyName(), journey.getCreatedBy() ,frontendLink );
            responses.add(response);
        }
        return responses;
    }

}
