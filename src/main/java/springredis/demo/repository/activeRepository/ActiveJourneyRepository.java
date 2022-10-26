package springredis.demo.repository.activeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Audience;
import springredis.demo.entity.activeEntity.ActiveJourney;

@Repository
public interface ActiveJourneyRepository extends JpaRepository<ActiveJourney, Long> {
}
