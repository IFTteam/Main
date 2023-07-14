package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Audience;
import springredis.demo.entity.User;

import java.util.List;

@Repository
public interface AudienceRepository extends JpaRepository<Audience, Long> {

    //equivalent to typed query with entitiy menager
    Audience findByEmail(String email);

    @Query(value="SELECT a from Audience a WHERE a.id=:Id")                 //search audience by id in audience repo
    Audience searchAudienceByid(long Id);

    @Query(value = "SELECT a from Audience a WHERE a.email=:Email")
    Audience searchAudienceByEmail(String Email);

    @Query("select s.id from Audience s where s.user = ?1")
    long getAudienceIdByUser(User user);

    @Query("select s from Audience s where s.user = ?1")
    List<Audience> getAudienceByUser(User user);

    Audience findById(long l);

}
