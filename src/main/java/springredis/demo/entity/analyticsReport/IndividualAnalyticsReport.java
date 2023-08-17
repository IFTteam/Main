package springredis.demo.entity.analyticsReport;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zeqing wang
 * AnalyticsReport for each journey
 */
@Data
@AllArgsConstructor
public class IndividualAnalyticsReport extends BaseInfo {
    private Map<String, TransmissionInfo> transmissionInfoMap;
    private List<String> emails;

    public IndividualAnalyticsReport() {
        transmissionInfoMap = new HashMap<>();
        emails = new ArrayList<>();
        totalClick = 0;
        totalBounce = 0;
        totalDelivery = 0;
        totalInjection = 0;
        totalOpen = 0;
    }
}
