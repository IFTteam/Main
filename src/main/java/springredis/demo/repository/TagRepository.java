package springredis.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Tag;
import springredis.demo.entity.User;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Tag findById(long l);

    @Query("select t from Tag t where t.user = ?1")
    List<Tag> getTagByUser(User user);
}
