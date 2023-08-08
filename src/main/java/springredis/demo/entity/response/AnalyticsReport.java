package springredis.demo.entity.response;

import lombok.Data;

@Data
public class AnalyticsReport {
    private Integer totalEmails;
    private Integer totalTransmissionIds;
    private Integer totalInjection;
    private Integer totalDelivery;
    private Integer totalOpen;
    private Integer totalClick;
    private Integer totalBounce;
}
