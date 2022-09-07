package springredis.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.activeEntity.ActiveJourney;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveJourneyRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;

@SpringBootTest
public class DemoApplicationTests {

    @Autowired
    private ActiveJourneyRepository activeJourneyRepository;

    @Autowired
    private ActiveNodeRepository activeNodeRepository;


    @Autowired
    private ActiveAudienceRepository activeAudienceRepository;
    @Test
    public void testOnetoMany(){
        ActiveJourney activeJourney = new ActiveJourney(123L);
        ActiveNode activeNode = new ActiveNode(3L,activeJourney);
        ActiveAudience activeAudience = new ActiveAudience(222L);
        activeJourney.getActiveNodeList().add(activeNode);
        activeNode.getActiveAudienceList().add(activeAudience);
        activeAudience.setActiveNode(activeNode);
        activeJourneyRepository.save(activeJourney);
        activeNodeRepository.save(activeNode);
        activeAudienceRepository.save(activeAudience);


    }



}