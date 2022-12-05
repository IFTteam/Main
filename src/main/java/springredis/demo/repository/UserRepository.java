package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Audience;
import springredis.demo.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value="SELECT u from User u WHERE u.id=:Id")                 //search audience by id in audience repo
    User searchUserById(long Id);

    User findByUsername(String userName);

    User findById(long l);
}
