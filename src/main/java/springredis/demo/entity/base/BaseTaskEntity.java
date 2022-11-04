package springredis.demo.entity.base;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@Data
public class BaseTaskEntity extends BaseEntity{

    //这三个是CoreModule的Key,无需处理直接返回即可
    //dont need to sort active audience info before calling task controller.
    private List<Long> activeAudienceId1 = new ArrayList<>();               //by defualt, everything is stored in 1. In if/else, audience that go to the other node is stored in the other list
    private List<Long> activeAudienceId2 = new ArrayList<>();
    /**
     * active_node 表的id
     */
    private Long targetNodeId;
    private Long sourceNodeId;
    //以下是主数据库Database对应的ID，可用于查询数据库Id
    private Long nodeId;
    private Long journeyId;
    private Long userId;
    private List<Long> audienceId1= new ArrayList<>();
    private List<Long> audienceId2= new ArrayList<>();

    public BaseTaskEntity(BaseTaskEntity baseTaskEntity){
        super();
        this.targetNodeId = baseTaskEntity.getTargetNodeId();
        this.sourceNodeId = baseTaskEntity.getSourceNodeId();
        this.nodeId = baseTaskEntity.getNodeId();
        this.journeyId = baseTaskEntity.getJourneyId();
        this.userId = baseTaskEntity.getUserId();
    }

}