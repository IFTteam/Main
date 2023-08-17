package springredis.demo.Service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import springredis.demo.Service.SparkPostAnalyticsService;
import springredis.demo.entity.Transmission;
import springredis.demo.entity.analyticsReport.TransmissionInfo;
import springredis.demo.entity.response.AnalyticsResponse;
import springredis.demo.entity.analyticsReport.IndividualAnalyticsReport;
import springredis.demo.repository.JourneyRepository;
import springredis.demo.repository.TransmissionRepository;

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

    private final JourneyRepository journeyRepository;

    private final TransmissionRepository transmissionRepository;

    private final String earliestStartTime = "2019-01-01T00:00";

    @Autowired
    public SparkPostAnalyticsServiceImpl(JourneyRepository journeyRepository, TransmissionRepository transmissionRepository) {
        this.journeyRepository = journeyRepository;
        this.transmissionRepository = transmissionRepository;
    }

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
        return getIndividualAnalyticsReport(analytics);
    }

    @Override
    public IndividualAnalyticsReport generateIndividualAnalyticsReportByJourneyId(Long journeyId) {
        List<Transmission> transmissionList = transmissionRepository.findByJourneyId(journeyId);
        StringBuilder sb = new StringBuilder();
        for (Transmission transmission : transmissionList) {
            sb.append(transmission.getId())
                    .append(",");
        }
        List<AnalyticsResponse> responses = getIndividualAnalyticsFromTransmissionID(sb.toString());
        return getIndividualAnalyticsReport(responses);
    }

    private IndividualAnalyticsReport getIndividualAnalyticsReport(List<AnalyticsResponse> analytics) {
        IndividualAnalyticsReport report = new IndividualAnalyticsReport();
        Set<String> emailSet = new HashSet<>();
        int totalJourneyInjection = 0;
        int totalJourneyDelivery = 0;
        int totalJourneyOpen = 0;
        int totalJourneyClick = 0;
        int totalJourneyBounce = 0;
        for (AnalyticsResponse analytic : analytics) {
            String transmissionId = analytic.getTransmission_id();
            String email = analytic.getRaw_rcpt_to();
            if (!report.getTransmissionInfoMap().containsKey(transmissionId)) {
                TransmissionInfo transmissionInfo = new TransmissionInfo();
                transmissionInfo.setTransmissionId(transmissionId);
                report.getTransmissionInfoMap().put(transmissionId, transmissionInfo);
            }

            emailSet.add(email);
            TransmissionInfo transmissionInfo = report.getTransmissionInfoMap().get(transmissionId);
            if ("injection".equals(analytic.getType())) {
                transmissionInfo.setTotalInjection(transmissionInfo.getTotalInjection() + 1);
                totalJourneyInjection++;
            }
            else if ("delivery".equals(analytic.getType())) {
                transmissionInfo.setTotalDelivery(transmissionInfo.getTotalDelivery() + 1);
                totalJourneyDelivery++;
            }
            else if ("bounce".equals(analytic.getType())) {
                transmissionInfo.setTotalBounce(transmissionInfo.getTotalBounce() + 1);
                totalJourneyBounce++;
            }
            else if ("open".equals(analytic.getType())) {
                transmissionInfo.setTotalOpen(transmissionInfo.getTotalOpen() + 1);
                totalJourneyOpen++;
            }
            else if ("click".equals(analytic.getType())) {
                transmissionInfo.setTotalClick(transmissionInfo.getTotalClick() + 1);
                totalJourneyClick++;
            }
            if (!transmissionInfo.getEmails().contains(email)) {
                transmissionInfo.getEmails().add(email);
            }
            report.getTransmissionInfoMap().put(transmissionId, transmissionInfo);
        }
        report.setEmails(new ArrayList<>(emailSet));
        report.setTotalBounce(totalJourneyBounce);
        report.setTotalDelivery(totalJourneyDelivery);
        report.setTotalClick(totalJourneyClick);
        report.setTotalInjection(totalJourneyInjection);
        report.setTotalOpen(totalJourneyOpen);
        return report;
    }

}
