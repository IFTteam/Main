package springredis.demo.repository.activeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Audience;
import springredis.demo.entity.Node;
import springredis.demo.entity.activeEntity.ActiveJourney;

@Repository
public interface ActiveJourneyRepository extends JpaRepository<ActiveJourney, Long> {

    @Query(value="SELECT j from ActiveJourney j WHERE j.journeyId=:journeyId")
    ActiveJourney searchActiveJourneyByJourneyId(Long journeyId);

}
