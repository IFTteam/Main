package springredis.demo.entity.analyticsReport;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zeqing wang
 * infomation required for each transmission
 */
@Data
public class TransmissionInfo extends BaseInfo {
    private String transmissionId;
    private List<String> emails;

    public TransmissionInfo() {
        emails = new ArrayList<>();
        totalDelivery = 0;
        totalClick = 0;
        totalBounce= 0;
        totalInjection = 0;
        totalOpen = 0;
    }
}
