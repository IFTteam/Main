package springredis.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.util.internal.StringUtil;
import lombok.Data;
import org.apache.tomcat.util.buf.StringUtils;
import org.yaml.snakeyaml.util.ArrayUtils;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.entity.base.BaseEntity;
import springredis.demo.entity.triggerType_node_relation;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Entity
@Data
@Table
public class Node extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    private Long frontEndId;
    private String name;
    private String type;

    private Integer headOrTail;
    private String status;

    // 下一个节点的id
    @ElementCollection
    private List<Long> nexts = new ArrayList<>();
    @ElementCollection
    private List<Long> lasts = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY,targetEntity = triggerType_node_relation.class)
    @JoinColumn(name="TNR_Node_id",referencedColumnName = "id")
    @JsonIgnore
    private triggerType_node_relation triggertype_node_relation;

    private String sNexts;
    private String sLasts;



    //Make Sure that sNexts is not empty when call this. sNexts->nexts
    public void nextsDeserialize(){
        nexts = new ArrayList<>();
        String[] s = sNexts.split(" ");
        for (String value : s) {
            if(!value.isEmpty()){
                nexts.add(Long.parseLong(value));
            }

        }
    }
    //nexts->sNexts
    public void nextsSerialize(){
        StringBuffer buffer = new StringBuffer();
        for(Long num:nexts){
            buffer.append(num.toString());
        }
        sNexts = buffer.toString();
    }

    public Node() {
        super();
    }

    public Node(String name, String type, String status, LocalDateTime createdAt, String createdBy, LocalDateTime updatedAt, String updatedBy) {
        super(createdAt, createdBy,updatedAt,updatedBy);
        this.name = name;
        this.type = type;
        this.status = status;
    }
}