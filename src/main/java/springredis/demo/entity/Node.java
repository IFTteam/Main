package springredis.demo.entity;

import io.netty.util.internal.StringUtil;
import lombok.Data;
import org.apache.tomcat.util.buf.StringUtils;
import org.yaml.snakeyaml.util.ArrayUtils;
import springredis.demo.entity.base.BaseEntity;

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

    @Transient
    private List<Long> nexts;
    @Transient
    private List<Long> lasts;

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