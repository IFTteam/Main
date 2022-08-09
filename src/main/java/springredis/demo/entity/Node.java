package springredis.demo.entity;

import lombok.Data;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;


@Entity
@Data
public class Node extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String type;
    private String status;




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
