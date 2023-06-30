package springredis.demo.Service;

import org.springframework.stereotype.Service;
import springredis.demo.entity.Journey;
import springredis.demo.entity.JourneyJsonModel;
import springredis.demo.error.JourneyNotFoundException;

import java.util.List;

/**
 * journey service layer
 * @author zeqing wang
 */
public interface JourneyService {

    /**
     * save the journey into repository
     * @param journeyJson given journey json
     * @return saved journey
     */
    Journey save(String journeyJson);

    /**
     * helper method to set journey status to the given status
     *
     * @param journeyJsonModel given journey json model
     * @param status           the status need to be set
     * @return saved journey
     * @throws JourneyNotFoundException if not found the journey by given journeyJsonModel
     */
    Journey setJourneyStatus(JourneyJsonModel journeyJsonModel, int status);

    /**
     * get Saved Journey By Journey Front End-Id
     * @param journeyFrontEndId given front end id
     * @return journey json
     */
    String getSavedJourneyByJourneyFrontEndId(String journeyFrontEndId);

    /**
     * activate the journey
     * @param journeyJson given journey json
     * @return activated journey
     */
    Journey activate(String journeyJson);

    Journey journeyParse(Journey journey);

    Boolean deleteActiveAudience(List<Long> activeNodeIdList);

    Boolean deleteActiveNodeAndJourney(Long JourneyId);

    Boolean endJourney(Long journeyId);
}
