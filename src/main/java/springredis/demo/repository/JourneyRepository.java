package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import springredis.demo.entity.Audience;
import springredis.demo.entity.Journey;
import springredis.demo.entity.Node;

import java.util.List;

public interface JourneyRepository extends JpaRepository<Journey, Long> {
    @Query(value="SELECT j from Journey j WHERE j.id=:Id")                 //search audience by id in audience repo
    Journey searchJourneyById(long Id);

}
