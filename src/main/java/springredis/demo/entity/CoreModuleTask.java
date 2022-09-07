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
    // core module also posts this task when calling other api
    @Id
    @GeneratedValue
    private Long id;
    //0 for move audience, 1 for create audience
    private int taskType;
    private String createModule;
    private String type;        //this is the general type of tasks
    private String name;        //this is the specific description of this task of this type; each api has its own coding
}
