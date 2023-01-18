package springredis.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.AudienceList;

@Repository
public interface AudienceListRepository extends JpaRepository<AudienceList,Long> {
}
