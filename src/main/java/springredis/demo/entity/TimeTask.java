package springredis.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.sql.In;
import springredis.demo.entity.base.BaseTaskEntity;

import javax.persistence.*;
import java.util.Date;



@Entity
@Data
@NoArgsConstructor
@Table(name="time_task")
public class TimeTask extends BaseTaskEntity {
    @Id
    @GeneratedValue
    @Column(name="id")
    private Long id;


    private Integer repeatTimes;
    private String repeatInterval;

    private Long triggerTime;

    private Integer taskStatus;

    public TimeTask(BaseTaskEntity baseTaskEntity){
        super(baseTaskEntity);
    }


    //status of the task
    //0-in sql db, 1-in heap, 2-task complete, -1-task cancelled
}