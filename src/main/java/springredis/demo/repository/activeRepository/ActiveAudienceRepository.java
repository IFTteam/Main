package springredis.demo.repository.activeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import springredis.demo.entity.activeEntity.ActiveAudience;

public interface ActiveAudienceRepository extends JpaRepository<ActiveAudience, Long> {

    @Query(value = "select t from ActiveAudience t where t.AudienceId = :audienceId")
    ActiveAudience findByDBId(Long audienceId);

    @Query(value = "select * from active_audience t where t.audience_node_id = :targetNodeId", nativeQuery = true)
    List<ActiveAudience> findByAudienceNodeId(@Param("targetNodeId") Long targetNodeId);
//    @Query(value="SELECT a FROM ActiveAudience a WHERE a.AudienceId=:audienceID")
//    Optional<ActiveAudience> searchActiveAudienceByAudienceId(long audienceID);
}
