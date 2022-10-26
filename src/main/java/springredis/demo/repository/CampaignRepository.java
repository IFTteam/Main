package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Campaign;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
}
