package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Journey;
import java.util.List;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, Long> {
    @Query(value="SELECT j from Journey j WHERE j.id=:Id")                 //search audience by id in audience repo
    Journey searchJourneyById(long Id);

    @Query(value="SELECT j from Journey j WHERE j.frontEndId=:frontEndId")
    Journey searchJourneyByFrontEndId(String frontEndId);

    Journey findById(long l);

    List<Journey> findByCreatedBy(String userid);



}
