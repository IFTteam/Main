package springredis.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.bind.v2.runtime.output.SAXOutput;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.Service.DAO;
import springredis.demo.entity.*;
import springredis.demo.repository.AudienceActivityRepository;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.TransmissionRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Slf4j
@RestController
@RequestMapping(path = "/analytics/webhook")
public class EventWebhookController {

    @Autowired
    AudienceRepository audienceRepository;

    @Autowired
    AudienceActivityRepository audienceActivityRepository;

    @Autowired
    TransmissionRepository transmissionRepository;

    @Autowired
    DAO productService;
    //respond to incoming webhook, uses transmission id to query transmission entity, get audience_id
    //and audience_email. Then insert into audience_activity table
    @RequestMapping(value = "/eventWebhook", method = POST)
    public ResponseEntity<Response> handleEventWebhook(HttpEntity<String> httpEntity) throws JsonProcessingException {
        //if(event.getDetails() == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response());
        String payload = httpEntity.getBody();
        log.info("Received event payload from SparkPost");
        JSONArray events = new JSONArray(payload);
        Long transmissionId = null;
        String eventType = null;
        // There are five categories, message_event, track_event, gen_event, unsubscribe_event, relay_event
        JSONObject category = null;
        String audienceEmail = null;

        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            JSONObject msys = event.optJSONObject("msys");
            System.out.println(msys + ": " + i);
//          Field: transmissionId, eventType,
            if ( msys!= null && msys.has("track_event")) {
                category = msys.optJSONObject("track_event");
            }
            if (category != null) {
                transmissionId = category.optLong("transmission_id");
                audienceEmail = category.getString("rcpt_to");
                Audience audience = productService.searchAudienceByEmail(audienceEmail);
                System.out.println("audience email: " + audience.getEmail());
                eventType = category.getString("type");
            }
            // we only want to keep track of click and open activities.
            if (eventType != null && (eventType.equals("open") || eventType.equals("click"))) {
                Transmission transmission = transmissionRepository.getReferenceById(transmissionId); // find transmission by id
                log.info("Transmission id is " + transmissionId);

                Audience audience = transmission.getAudience();
                AudienceActivity audienceActivity = new AudienceActivity();
                audienceActivity.setAudience(audience);
                audienceActivity.setEventType(eventType);
                audienceActivity.setAudience_email(audience.getEmail());
                audienceActivity.setCreatedAt(LocalDateTime.now());
                audienceActivity.setCreatedBy("SparkPost");
                audienceActivityRepository.save(audienceActivity);
                System.out.println("audience activity added to DB");

            }
        }

//        int transmissionId = event.getDetails().get("transmission_id").asInt();
//        String eventType = event.getDetails().get("type").asText();
//        int transmissionId = Integer.parseInt(event.getDetails().get("transmission_id").toString());
//        String eventType = event.getDetails().get("type").toString();
//        String transmissionId = event.getMsys().getEventDetail().getTransmissionId();
//        String eventType = event.getMsys().getEventDetail().getType();


//        Transmission transmission = new Transmission();
//        Optional<Transmission> optionalTransmission = transmissionRepository.findById(Long.valueOf(transmissionId));
//        if (optionalTransmission.isPresent()) {
//            transmission = optionalTransmission.get();
//        } else {
//            transmission.setId(transmissionId);
////            transmission.setUser();
////            transmission.setJourney();
////            transmission.setAudience();
////            transmission.setAudience_email();
//
//            transmissionRepository.save(transmission);
//        }



        // Spark post requires 200 status code
        Response response = new Response(); // 直接把status code設定成200可能有問題，需要catch error等等...
        response.setStatusCode("200");
        response.setStatusMsg("Event data received!");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
