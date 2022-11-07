package springredis.demo.entity;

import lombok.Data;
import springredis.demo.entity.base.BaseTaskEntity;

import javax.persistence.*;

@Data
@Entity
public class CoreModuleTask extends BaseTaskEntity {
    // Return this Entity when call core module
    //  also posts this task when calling other api
    @Id
    @GeneratedValue
    private Long id;
    private int taskType;       //0 for move audience, 1 for create audience
    private String createModule;
    private String type;        //this is the general type of tasks
    private String name;        //this is the specific description of this task of this type; each api has its own coding
    private int makenext=1;     //If set to 1, means the task (when returned to core module) need to make the next task based on next nodes; if 0 then core module will not make a new task when this task is returned
    private int callapi=1;      //If this is 1 (by default), CMTEexecutor will call the respective API for this task. Else, it will simply transfer audience from this task's node to next node, and make next node's task
    
	public CoreModuleTask(BaseTaskEntity baseTaskEntity) {
		super(baseTaskEntity);
		// TODO Auto-generated constructor stub
	}
	
	public CoreModuleTask() {
		super();
	}
}
