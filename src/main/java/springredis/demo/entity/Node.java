package springredis.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import springredis.demo.entity.base.BaseEntity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CascadeType;

@Entity
@Data
@Table
public class Node extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String frontEndId; //改成string
    private String name;
    private String type;

    private Integer headOrTail; // what if there's only one node
    private String status;
    private String properties;
    private String journeyFrontEndId;
    private Integer endNodesCount;

    @ElementCollection
    @JoinColumn(name = "node_id")
    // 需要先删除已有的表，然后build
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Cascade(value = {CascadeType.ALL})
    private List<Long> nexts = new ArrayList<>();

    @ElementCollection
    @JoinColumn(name = "node_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Cascade(value = {CascadeType.ALL})
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
        if (sNexts == null) return;
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
        if (nexts == null) {
            sNexts = "";
            return;
        }
        System.out.println(nexts);
        for(Long num:nexts){
            buffer.append(num.toString());
            buffer.append(" ");  //separate by spaces for deserializatiing
        }
        sNexts = buffer.toString();
    }

    public Node() {
        super();
    }

    public Node(String name,
                String type,
                String status,
                LocalDateTime createdAt,
                String createdBy,
                LocalDateTime updatedAt,
                String updatedBy,
                String journeyFrontEndId,
                String properties) {
        super(createdAt, createdBy, updatedAt,updatedBy);
        this.name = name;
        this.type = type;
        this.status = status;
        this.journeyFrontEndId = journeyFrontEndId;
        this.properties = properties;
    }
}