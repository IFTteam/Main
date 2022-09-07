package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import springredis.demo.entity.triggerType_node_relation;

import java.util.Optional;

public interface TNR_Repository extends JpaRepository<triggerType_node_relation, Long> {
    @Query(value = "SELECT t from TNR t WHERE t.userId=:uid AND t.triggerType=:type ",nativeQuery = true)
    public Optional<triggerType_node_relation> searchTNR(Long uid, String type);

}
