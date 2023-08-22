package springredis.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class triggerType_node_relation {
    @Id
    @GeneratedValue
    private long id;

    private String triggerType;         //three types: purchase, abandon_cart, subscription

    private Long userId;                //the userid in main table


    @OneToMany(mappedBy="triggertype_node_relation",fetch = FetchType.LAZY,cascade= CascadeType.ALL)
    private List<Node> nodes = new ArrayList<>();                   //the list of active nodes for this user's this journey's this trigger type; when trigger is invoked,

    public triggerType_node_relation(String s, long uid){
        triggerType = s;
        userId = uid;
    }

    public void setNode(Node node){
        nodes = new ArrayList<>();
        nodes.add(node);
    }

}
