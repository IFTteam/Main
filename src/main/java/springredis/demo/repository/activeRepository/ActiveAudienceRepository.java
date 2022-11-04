package springredis.demo.repository.activeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.ResponseBody;
import springredis.demo.entity.Audience;
import springredis.demo.entity.activeEntity.ActiveAudience;

import java.util.List;
import java.util.Optional;

import static org.hibernate.loader.Loader.SELECT;

@Repository
public interface ActiveAudienceRepository extends JpaRepository<ActiveAudience, Long> {

    @Query(value = "select t from ActiveAudience t where t.AudienceId = :audienceId")
    ActiveAudience findByDBId(Long audienceId);

    @Query(value = "select * from active_audience t where t.audience_node_id = :targetNodeId", nativeQuery = true)
    List<ActiveAudience> findByAudienceNodeId(@Param("targetNodeId") Long targetNodeId);

//    @Query(value="SELECT a FROM ActiveAudience a WHERE a.AudienceId=:audienceID")
//    Optional<ActiveAudience> searchActiveAudienceByAudienceId(long audienceID);
}
