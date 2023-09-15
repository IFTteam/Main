package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.AudienceAudiencelist;
import springredis.demo.entity.AudienceAudiencelistId;

@Repository
public interface AudienceAudiencelistRepository extends JpaRepository<AudienceAudiencelist, AudienceAudiencelistId> {


}
