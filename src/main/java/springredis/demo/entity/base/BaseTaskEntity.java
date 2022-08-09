package springredis.demo.entity.base;

public class BaseTaskEntity {

    //这三个是CoreModule的Key,无需处理直接返回即可
    private Long activeAudienceId;
    private Long targetNodeId;
    private Long sourceNodeId;
    //以下是主数据库Database对应的ID，可用于查询数据库Id
    private Long nodeId;
    private Long journeyId;
    private Long userId;
    private Long audienceId;
}
