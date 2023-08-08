package springredis.demo.Service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import springredis.demo.Service.SparkPostAnalyticsService;
import springredis.demo.entity.response.AnalyticsReport;
import springredis.demo.entity.response.AnalyticsResponse;
import springredis.demo.entity.response.IndividualAnalyticsReport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SparkPostAnalyticsServiceImpl implements SparkPostAnalyticsService {

    @Value("${SPARKPOST_API_KEY}")
    private String SPARKPOST_API_KEY;

    @Value("${SP_BASE_URI}")
    private String sparkPostBaseUrl;

    private final String earliestStartTime = "2019-01-01T00:00";

    public List<AnalyticsResponse> getAnalytics(String url)
    {
        WebClient client = WebClient.create(url);
        HttpHeaders headers = new HttpHeaders();

        headers.setBasicAuth(SPARKPOST_API_KEY);

        ResponseEntity<String> Response = client.get()
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .toEntity(String.class)
                .block();

        assert Response != null;
        String responseBody = Response.getBody();
        HttpStatus statusCode = Response.getStatusCode();

        // Process the response body
        JSONObject json = new JSONObject(responseBody);
        JSONArray resultList = json.getJSONArray("results");

        Type listOfMyClassObject = new TypeToken<ArrayList<AnalyticsResponse>>() {
        }.getType();

        Gson gson = new Gson();
        List<AnalyticsResponse> AnalyticsResponseList = gson.fromJson(resultList.toString(), listOfMyClassObject);


        return AnalyticsResponseList;
    }

    @Override
    public List<AnalyticsResponse> getIndividualAnalyticsFromRecipientEmail(String email) {
        String url = sparkPostBaseUrl + "/api/v1/events/message?" +
                "from=" + earliestStartTime +
                "&recipients=" + email;
        return getAnalytics(url);

    }

    @Override
    public List<AnalyticsResponse> getIndividualAnalyticsFromTransmissionID(String transmissionId) {
        String url = sparkPostBaseUrl + "/api/v1/events/message?" +
                "from=" + earliestStartTime +
                "&transmissions=" + transmissionId;
        return getAnalytics(url);
    }

    @Override
    public List<AnalyticsResponse> getAllAnalytics() {
        String url = sparkPostBaseUrl + "/api/v1/events/message?" +
                "from=" + earliestStartTime;
        return getAnalytics(url);
    }

    @Override
    public IndividualAnalyticsReport generateIndividualAnalyticsReportFromRecipientEmail(String recipientEmail) {
        List<AnalyticsResponse> analytics = getIndividualAnalyticsFromRecipientEmail(recipientEmail);
        return getIndividualAnalyticsReport(analytics, recipientEmail);
    }

    @Override
    public AnalyticsReport generateAnalyticsReport() {
        List<AnalyticsResponse> analytics = getAllAnalytics();
        return getAnalyticsReport(analytics);
    }


    private AnalyticsReport getAnalyticsReport(List<AnalyticsResponse> analytics) {
        AnalyticsReport analyticsReport = new AnalyticsReport();
        int totalInjection = 0;
        int totalDelivery = 0;
        int totalOpen = 0;
        int totalClick = 0;
        int totalBounce = 0;
        Set<String> emailSet = new HashSet<>();
        Set<String> transmissionsSet = new HashSet<>();
        for (AnalyticsResponse analytic : analytics) {
            emailSet.add(analytic.getRaw_rcpt_to());
            transmissionsSet.add(analytic.getTransmission_id());
        }

        for (String email : emailSet) {
            IndividualAnalyticsReport individualAnalyticsReport = generateIndividualAnalyticsReportFromRecipientEmail(email);
            totalBounce += individualAnalyticsReport.getTotalBounce();
            totalClick += individualAnalyticsReport.getTotalClick();
            totalDelivery += individualAnalyticsReport.getTotalDelivery();
            totalOpen += individualAnalyticsReport.getTotalOpen();
            totalInjection += individualAnalyticsReport.getTotalInjection();
        }

        analyticsReport.setTotalEmails(emailSet.size());
        analyticsReport.setTotalTransmissionIds(transmissionsSet.size());
        analyticsReport.setTotalInjection(totalInjection);
        analyticsReport.setTotalDelivery(totalDelivery);
        analyticsReport.setTotalClick(totalClick);
        analyticsReport.setTotalOpen(totalOpen);
        analyticsReport.setTotalBounce(totalBounce);
        return analyticsReport;
    }

    private IndividualAnalyticsReport getIndividualAnalyticsReport(List<AnalyticsResponse> analytics, String recipientEmail) {
        IndividualAnalyticsReport report = new IndividualAnalyticsReport();
        report.setEmail(recipientEmail);
        List<String> transmissionIds = new ArrayList<>();
        int totalInjection = 0;
        int totalDelivery = 0;
        int totalOpen = 0;
        int totalClick = 0;
        int totalBounce = 0;
        for (AnalyticsResponse analytic : analytics) {
            transmissionIds.add(analytic.getRaw_rcpt_to());
            if ("injection".equals(analytic.getType())) {
                totalInjection++;
            }
            else if ("delivery".equals(analytic.getType())) {
                totalDelivery++;
            }
            else if ("bounce".equals(analytic.getType())) {
                totalBounce++;
            }
            else if ("open".equals(analytic.getType())) {
                totalOpen++;
            }
            else if ("click".equals(analytic.getType())) {
                totalClick++;
            }
        }

        report.setTransmissionIds(transmissionIds);
        report.setTotalBounce(totalBounce);
        report.setTotalDelivery(totalDelivery);
        report.setTotalClick(totalClick);
        report.setTotalInjection(totalInjection);
        report.setTotalOpen(totalOpen);
        return report;
    }

}
