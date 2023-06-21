package springredis.demo.repository.activeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.activeEntity.ActiveAudience;

import java.util.List;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveAudienceRepository extends JpaRepository<ActiveAudience, Long> {

    @Query(value = "select t from ActiveAudience t where t.AudienceId = :audienceId")
    ActiveAudience findByDBId(Long audienceId);

    @Query(value = "select t from ActiveAudience t where t.AudienceId = :audienceId")
    List<ActiveAudience> findByAudienceId(Long audienceId);

    @Transactional
    @Modifying
    long deleteByActiveNodeId(Long id);

    @Query(value = "SELECT A from ActiveAudience A WHERE A.id = :ID")
    ActiveAudience searchActiveAudienceByid(Long ID);
//    @Query(value="SELECT a FROM ActiveAudience a WHERE a.AudienceId=:audienceID")
//    Optional<ActiveAudience> searchActiveAudienceByAudienceId(long audienceID);

    @Query(value = "select t from ActiveAudience t where t.AudienceId = :audienceId")
    List<ActiveAudience> findByAudienceId(Long audienceId);

    @Modifying
    @Query(nativeQuery = true, value = "delete from ActiveAudience where  audience_id = ?1  and audience_node_id = ?2")
    void deleteWhenEndNode(Long audienceId, Long audienceNodeId);


}
