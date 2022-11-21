package springredis.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.Response.JourneyResponse;
import springredis.demo.Service.DashBoardService;
import springredis.demo.entity.Journey;
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
    @GetMapping("/journey/all/{userid}")
    public List<List<String>> getAllJourneys(@PathVariable("userid") String id){
        return dashBoardService.findAllJourneyById(id);
    }

    @GetMapping("/journey/all")
    public List<JourneyResponse> getJourneys(){
        List<Journey> journeys = journeyRepository.findAll();
        List<JourneyResponse> responses=new ArrayList<>();
        for(Journey journey: journeys) {
            JourneyResponse response = new JourneyResponse(journey.getId(), journey.getJourneyName(), journey.getCreatedBy(), journey.getFrontEndId() );
            responses.add(response);
        }
        return responses;
    }

}
