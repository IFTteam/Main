package springredis.demo.Service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.entity.Journey;
import springredis.demo.repository.JourneyRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class DashBoardServiceImpl implements DashBoardService{

    @Autowired
    private JourneyRepository journeyRepository;

    @Override
    public List<List<String>> findAllJourneyById(String userid) {
        List<List<String>> journeys = new ArrayList<>();
        List<Journey> allJourney = journeyRepository.findByCreatedBy(userid);
        for (Journey journey: allJourney){
            List<String> myJourney = new ArrayList<>();
            myJourney.add(journey.getJourneyName());
            myJourney.add(journey.getFrontEndId());
            journeys.add(myJourney);
        }
        return journeys;
    }
}
