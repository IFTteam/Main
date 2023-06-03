package springredis.demo.repository.activeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.activeEntity.ActiveAudience;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
public interface ActiveAudienceRepository extends JpaRepository<ActiveAudience, Long> {

    @Query(value = "select t from ActiveAudience t where t.AudienceId = :audienceId")
    ActiveAudience findByDBId(Long audienceId);
    @Transactional
    @Modifying
    long deleteByActiveNodeId(Long id);

    @Query(value = "select t from ActiveAudience t where t.AudienceId = :audienceId")
    List<ActiveAudience> findByAudienceId(Long audienceId);

    @Query(value = "SELECT A from ActiveAudience A WHERE A.id = :ID")
    ActiveAudience searchActiveAudienceByid(Long ID);
//    @Query(value="SELECT a FROM ActiveAudience a WHERE a.AudienceId=:audienceID")
//    Optional<ActiveAudience> searchActiveAudienceByAudienceId(long audienceID);
}
