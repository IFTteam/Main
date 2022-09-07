package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import springredis.demo.entity.Audience;

import java.util.Optional;

public interface AudienceRepository extends JpaRepository<Audience, Long> {
    //equivalent to typed query with entitiy menager

@Query(value="SELECT a from Audience a WHERE a.id=:Id")                 //search audience by id in audience repo
Audience searchAudienceByid(long Id);

    @Query(value = "SELECT a from Audience a WHERE a.email=:Email")
    Optional<Audience> searchAudienceByEmail(String Email);

}
