package springredis.demo.entity.analyticsReport;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zeqing wang
 * base infomation for analyticsReport
 */
@Data
@NoArgsConstructor
public class BaseInfo {
    protected Integer totalInjection;
    protected Integer totalDelivery;
    protected Integer totalOpen;
    protected Integer totalClick;
    protected Integer totalBounce;
}
