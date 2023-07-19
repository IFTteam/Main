package springredis.demo.repository.activeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.activeEntity.ActiveNode;

import javax.transaction.Transactional;
import java.util.List;

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
    @Query(value="DELETE FROM ActiveNode t WHERE t.activeJourney.id=:nodeJourneyId")
    void deleteByNodeJourneyId(Long nodeJourneyId);

    @Transactional
    @Modifying
    @Query(value="select t from ActiveNode t where t.activeJourney.id=:nodeJourneyId")
    List<ActiveNode> searchByNodeJourneyId(Long nodeJourneyId);
}
