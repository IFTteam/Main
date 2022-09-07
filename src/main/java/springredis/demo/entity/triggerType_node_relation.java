package springredis.demo.entity;

import lombok.Data;
import lombok.Generated;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Data
@Table(name="TNR")
public class triggerType_node_relation extends BaseEntity {
    @Id
    @GeneratedValue
    private long id;

    private String triggerType;         //three types: purchase, abandon_cart, subscription

    private Long userId;                //the userid in main table


    @OneToMany(mappedBy="triggerType_node_relation")
    private List<Node> nodes;                   //the list of active nodes for this user's this journey's this trigger type; when trigger is invoked,

    public triggerType_node_relation(String s, long uid){
        triggerType = s;
        userId = uid;
        nodes = new ArrayList<>();
    }

    public void addnode(Node node){
        nodes.add(node);
    }

}
