package springredis.demo.Service;

import springredis.demo.entity.response.AnalyticsResponse;
import springredis.demo.entity.analyticsReport.IndividualAnalyticsReport;

import java.util.List;

public interface SparkPostAnalyticsService {

    List<AnalyticsResponse> getAnalytics(String url);

    List<AnalyticsResponse> getIndividualAnalyticsFromRecipientEmail(String email);

    List<AnalyticsResponse> getIndividualAnalyticsFromTransmissionID(String transmissionId);

    List<AnalyticsResponse> getAllAnalytics();

    IndividualAnalyticsReport generateIndividualAnalyticsReportFromRecipientEmail(String recipientEmail);

    IndividualAnalyticsReport generateIndividualAnalyticsReportByJourneyId(Long journeyId);
}
