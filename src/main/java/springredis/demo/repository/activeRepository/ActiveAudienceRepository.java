package springredis.demo.repository.activeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.activeEntity.ActiveAudience;

import java.util.List;
import javax.transaction.Transactional;

@Repository
public interface ActiveAudienceRepository extends JpaRepository<ActiveAudience, Long> {

    @Query(value = "select t from ActiveAudience t where t.AudienceId = :audienceId")
    ActiveAudience findByDBId(Long audienceId);

    @Transactional
    @Modifying
    long deleteByActiveNodeId(Long id);

    @Query(value = "select t from ActiveAudience t where t.AudienceId = :audienceId")
    List<ActiveAudience> findByAudienceId(Long audienceId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "delete from active_audience where audience_node_id in ?1")
    void deleteWhenEndNode(List<Long> activeNodeIdList);

}
