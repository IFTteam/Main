package springredis.demo.repository.activeRepository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import springredis.demo.entity.Audience;
import springredis.demo.entity.activeEntity.ActiveAudience;

import java.util.Optional;

import static org.hibernate.loader.Loader.SELECT;

public interface ActiveAudienceRepository extends JpaRepository<ActiveAudience, Long> {

    @Query(value = "select * from active_audience t where t.audience_id = audience_id",nativeQuery = true)
    ActiveAudience findByDBId(@Param("audience_id") Long audienceId);
    @Query(value="SELECT a FROM ActiveAudience a WHERE a.AudienceId=:audienceID")
    Optional<ActiveAudience> searchActiveAudienceByAudienceId(long audienceID);
}
