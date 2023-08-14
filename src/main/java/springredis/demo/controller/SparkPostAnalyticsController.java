package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.Service.SparkPostAnalyticsService;
import springredis.demo.entity.response.AnalyticsResponse;
import lombok.extern.slf4j.Slf4j;
import springredis.demo.entity.response.IndividualAnalyticsReport;

import java.util.List;

@Slf4j // logger
@RestController
@RequestMapping(path = "/analytics")
public class SparkPostAnalyticsController {
    private final SparkPostAnalyticsService sparkPostAnalyticsService;

    @Autowired
    public SparkPostAnalyticsController(SparkPostAnalyticsService sparkPostAnalyticsService) {
        this.sparkPostAnalyticsService = sparkPostAnalyticsService;
    }

    @GetMapping("/individual/email/{email}")
    public List<AnalyticsResponse> getIndividualAnalyticsEmail(@PathVariable("email") String audienceEmail) {
        log.info("begin to get sparkpost individual analytics from recipient email: {}...", audienceEmail);
        return sparkPostAnalyticsService.getIndividualAnalyticsFromRecipientEmail(audienceEmail);
    }

    @GetMapping("/individual/transmission/{transmissionID}")
    public List<AnalyticsResponse> getIndividualAnalyticsTransmissionID(@PathVariable String transmissionID) {
        log.info("begin to get sparkpost individual analytics from transmission id: {}...", transmissionID);
        return sparkPostAnalyticsService.getIndividualAnalyticsFromTransmissionID(transmissionID);
    }

    @GetMapping
    public List<AnalyticsResponse> getAllAnalytics() {
        log.info("begin to get sparkpost all analytics...");
        return sparkPostAnalyticsService.getAllAnalytics();
    }


    @GetMapping("/individual/report/email/{email}")
    public IndividualAnalyticsReport getIndividualAnalyticsReportRecipientEmail(@PathVariable String email) {
        log.info("begin to get sparkpost individual analytics report from recipient email: {}...", email);
        return sparkPostAnalyticsService.generateIndividualAnalyticsReportFromRecipientEmail(email);
    }


    @GetMapping("/individual/report/journey/{journeyId}")
    public IndividualAnalyticsReport getIndividualAnalyticsReportByJourneyId(@PathVariable Long journeyId) {
        log.info("begin to get sparkpost individual analytics report from journey: {}", journeyId);
        return sparkPostAnalyticsService.generateIndividualAnalyticsReportByJourneyId(journeyId);
    }
}
