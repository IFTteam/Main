package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Audience;
import springredis.demo.entity.Journey;
import springredis.demo.entity.Node;

import java.util.List;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {

    @Query(value="SELECT n from Node n WHERE n.id=:Id")                 //search audience by id in audience repo
    Node searchNodeByid(long Id);

    @Query(value="SELECT n from Node n WHERE n.frontEndId=:frontEndId")
    Node searchNodeByFrontEndId(String frontEndId);

    @Query(value="SELECT n from Node n WHERE n.journeyFrontEndId=:journeyFrontEndId")
    Node[] searchNodesByJourneyFrontEndId(String journeyFrontEndId);

    @Query(value="SELECT n from Node n WHERE n.createdBy=:createdBy AND n.name=:nodeType")
    List<Node> searchNodesByCreatedByAndName(String createdBy, String nodeType);

    @Query(value="SELECT n from Node n WHERE n.journeyFrontEndId=:journeyFrontEndId AND n.name=:nodeName")
    Node searchByJourneyFrontEndIdAndName(String journeyFrontEndId, String nodeName);

//    @Query(value="SELECT n from Node n WHERE n.journeyFrontEndId=:journeyFrontEndId AND n.headOrTail=:headOrTail")
//    Node searchByJourneyFrontEndIdAndHeadOrTail(String journeyFrontEndId, Integer headOrTail);
}
