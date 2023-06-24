package springredis.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.Service.JourneyService;
import springredis.demo.entity.*;
import springredis.demo.error.JourneyNotFoundException;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("journey")
@Slf4j
public class JourneyController {
    private final JourneyService journeyService;

    @Autowired
    public JourneyController(JourneyService journeyService) {
        this.journeyService = journeyService;
    }

    @PostMapping("/saveJourney")//保存Journey,仅仅保存Serialized部分
    public Journey saveJourney(@RequestBody String journeyJson) {
        log.info("begin to save the journey...");
        return journeyService.save(journeyJson);
    }

    /**
     * a put mapping to update the current journey status to the newest status code
     *
     * @param journeyJsonModel given parameter
     * @return a Journey object follow the previous functions mode
     * @throws JourneyNotFoundException if not found by the given journey front-end-id,
     *                                  then throw this exception
     */
    @PutMapping("/status")
    public Journey changeJourneyStatus(@RequestBody JourneyJsonModel journeyJsonModel) throws JourneyNotFoundException {
        log.info("begin to change journey status...");
        log.info(journeyJsonModel.toString());

        // get status code and front end id from given journey json model
        int status = journeyJsonModel.getProperties().getStatus();

        // call set journey status helper method
        return journeyService.setJourneyStatus(journeyJsonModel, status);
    }

    @GetMapping("/get-saved-journey/{journeyFrontEndId}")//激活Journey,查取数据库，反序列化
    public String getSavedJourney(@PathVariable String journeyFrontEndId) {
        log.info("begin to search the journey by given journey front-end-id...");
        return journeyService.getSavedJourneyByJourneyFrontEndId(journeyFrontEndId);
    }

    @PostMapping("/activateJourney")//激活Journey,查取数据库，反序列化
    public Journey activateJourney(@RequestBody String journeyJson) throws JourneyNotFoundException {
        log.info("begin to activate the journey...");
        log.info(journeyJson);
        return journeyService.activate(journeyJson);
    }
}