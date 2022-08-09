package springredis.demo.entity;

import lombok.Data;
import springredis.demo.entity.base.BaseTaskEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
public class CoreModuleTask extends BaseTaskEntity {
    // Return this Entity when call core module

    @Id
    private Long id;
    //0 for move audience, 1 for create audience
    private int taskType;
    private String createModule;
    //the nodeId

}
