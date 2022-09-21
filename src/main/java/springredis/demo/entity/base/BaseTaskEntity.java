package springredis.demo.entity.base;

import lombok.NoArgsConstructor;


@NoArgsConstructor
public class BaseTaskEntity extends BaseEntity{

    //这三个是CoreModule的Key,无需处理直接返回即可
    private Long activeAudienceId;
    private Long targetNodeId;
    private Long sourceNodeId;
    //以下是主数据库Database对应的ID，可用于查询数据库Id
    private Long nodeId;
    private Long journeyId;
    private Long userId;
    private Long audienceId;

    public BaseTaskEntity(BaseTaskEntity baseTaskEntity){
        super();
        this.activeAudienceId = baseTaskEntity.getActiveAudienceId();
        this.targetNodeId = baseTaskEntity.getTargetNodeId();
        this.sourceNodeId = baseTaskEntity.getSourceNodeId();
        this.nodeId = baseTaskEntity.getNodeId();
        this.journeyId = baseTaskEntity.getJourneyId();
        this.userId = baseTaskEntity.getUserId();
        this.audienceId = baseTaskEntity.getAudienceId();
    }

    public Long getActiveAudienceId() {
        return activeAudienceId;
    }

    public void setActiveAudienceId(Long activeAudienceId) {
        this.activeAudienceId = activeAudienceId;
    }

    public Long getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(Long targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public Long getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(Long sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Long getJourneyId() {
        return journeyId;
    }

    public void setJourneyId(Long journeyId) {
        this.journeyId = journeyId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAudienceId() {
        return audienceId;
    }

    public void setAudienceId(Long audienceId) {
        this.audienceId = audienceId;
    }
}