package springredis.demo.repository.activeRepository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Audience;
import springredis.demo.entity.activeEntity.ActiveNode;

@Repository
public interface ActiveNodeRepository extends JpaRepository<ActiveNode, Long> {
    @Query(value = "select t from ActiveNode t where t.nodeId = :node_id")
    ActiveNode findByDBNodeId(Long node_id);

    @Query(value="select t from ActiveNode t where t.id=:Id")
    ActiveNode findByActiveNodeId(Long Id);
}
