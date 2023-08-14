package springredis.demo.entity.response;

import lombok.Data;

import java.util.List;

@Data
public class IndividualAnalyticsReport {
    private List<String> emails;
    private List<String> transmissionIds;
    private Integer totalInjection;
    private Integer totalDelivery;
    private Integer totalOpen;
    private Integer totalClick;
    private Integer totalBounce;
}
