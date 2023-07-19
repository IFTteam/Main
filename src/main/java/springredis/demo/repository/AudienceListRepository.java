package springredis.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.AudienceList;
import springredis.demo.entity.User;

import java.util.List;

@Repository
public interface AudienceListRepository extends JpaRepository<AudienceList,Long> {

    @Query(value="SELECT n from AudienceList n WHERE n.audienceListName=:audienceListName")
    AudienceList searchAudienceListByName(String audienceListName);

    @Query(value="SELECT n from AudienceList n WHERE n.user=:user")
    List<AudienceList> findByUser(User user);

    List<AudienceList> findAll();

}
