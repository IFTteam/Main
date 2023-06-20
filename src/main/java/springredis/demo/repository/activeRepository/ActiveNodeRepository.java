package springredis.demo.repository.activeRepository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Audience;
import springredis.demo.entity.activeEntity.ActiveNode;

import javax.transaction.Transactional;

@Repository
public interface ActiveNodeRepository extends JpaRepository<ActiveNode, Long> {
    @Query(value = "select t from ActiveNode t where t.nodeId = :node_id")
    ActiveNode findByDBNodeId(Long node_id);

    @Query(value="select t from ActiveNode t where t.id=:Id")
    ActiveNode findByActiveNodeId(Long Id);

    @Transactional
    @Modifying
    @Query(value="DELETE FROM ActiveNode t WHERE t.nodeId=:nodeId")
    void deleteByNodeId(Long nodeId);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value ="delete from active_node t where t.node_journey_id = ?1")
    void deleteByNodeJourneyId(Long nodeJourneyId);

}
