package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.Service.SparkPostAnalyticsService;
import springredis.demo.entity.response.AnalyticsReport;
import springredis.demo.entity.response.AnalyticsResponse;
import lombok.extern.slf4j.Slf4j;
import springredis.demo.entity.response.IndividualAnalyticsReport;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j // logger
@RestController
@RequestMapping(path = "/analytics")
public class SparkPostAnalyticsController {
    private final SparkPostAnalyticsService sparkPostAnalyticsService;

    @Autowired
    public SparkPostAnalyticsController(SparkPostAnalyticsService sparkPostAnalyticsService) {
        this.sparkPostAnalyticsService = sparkPostAnalyticsService;
    }

    @RequestMapping(value={"/individual/email/{email}"}, method = GET)
    @ResponseBody
    public List<AnalyticsResponse> getIndividualAnalyticsEmail(@PathVariable("email") String audienceEmail) {
        log.info("begin to get sparkpost individual analytics from recipient email: {}...", audienceEmail);
        return sparkPostAnalyticsService.getIndividualAnalyticsFromRecipientEmail(audienceEmail);
    }

    @RequestMapping(value={"/individual/transmission/{transmissionID}"}, method = GET)
    @ResponseBody
    public List<AnalyticsResponse> getIndividualAnalyticsTransmissionID(@PathVariable("transmissionID") String transmissionID) {
        log.info("begin to get sparkpost individual analytics from transmission id: {}...", transmissionID);
        return sparkPostAnalyticsService.getIndividualAnalyticsFromTransmissionID(transmissionID);
    }

    @GetMapping
    public List<AnalyticsResponse> getAllAnalytics() {
        log.info("begin to get sparkpost all analytics...");
        return sparkPostAnalyticsService.getAllAnalytics();
    }


    @GetMapping("/individual/report/{email}")
    public IndividualAnalyticsReport getIndividualAnalyticsReportRecipientEmail(String recipientEmail) {
        log.info("begin to get sparkpost individual analytics report from recipient email: {}...", recipientEmail);
        return sparkPostAnalyticsService.generateIndividualAnalyticsReportFromRecipientEmail(recipientEmail);
    }

    @GetMapping("/report")
    public AnalyticsReport getAnalyticsReport() {
        log.info("begin to get sparkpost all analytics report...");
        return sparkPostAnalyticsService.generateAnalyticsReport();
    }
}
