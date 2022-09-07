package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import springredis.demo.entity.Audience;
import springredis.demo.entity.Node;

public interface NodeRepository extends JpaRepository<Node, Long> {
    @Query(value="SELECT n from Node n WHERE n.id=:Id")                 //search audience by id in audience repo
    Node searchNodeByid(long Id);
}
