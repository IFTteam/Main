package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Journey;
import springredis.demo.entity.TagDetail;

import java.util.Optional;

@Repository
public interface TagDetailRepository extends JpaRepository<TagDetail,Long> {

    @Query("select t from TagDetail t where t.journey=:journey")
    Optional<TagDetail> findByJourney(Journey journey);
}
