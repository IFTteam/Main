package springredis.demo.repository.activeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import springredis.demo.entity.Audience;
import springredis.demo.entity.activeEntity.ActiveAudience;

import java.util.Optional;

import static org.hibernate.loader.Loader.SELECT;

public interface ActiveAudienceRepository extends JpaRepository<ActiveAudience, Long> {
    @Query(value="SELECT a FROM ActiveAudience a WHERE a.AudienceId=:audienceID")
    Optional<ActiveAudience> searchActiveAudienceByAudienceId(long audienceID);
}
