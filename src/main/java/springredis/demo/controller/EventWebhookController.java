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
            if ( msys!= null && msys.has("track_event")) { // 1st checkpoint: track_event
                category = msys.optJSONObject("track_event");
            }
            if (category != null) {
                transmissionId = category.optLong("transmission_id");
//                System.out.println(transmissionId); // print transmission Id
                audienceEmail = category.getString("rcpt_to");
//                System.out.println(audienceEmail);
                Audience audience = productService.searchAudienceByEmail(audienceEmail);

//                if (audience != null) {
//                    System.out.println("audience email: " + audience.getEmail()); // print audience email
//                }
                eventType = category.getString("type");
//                System.out.println(eventType); // print event type
            }
            // we only want to keep track of click and open activities.
            if (eventType != null && (eventType.equals("open") || eventType.equals("click"))) { // 2nd checkpoint: open or click event
                Optional<Transmission> transmission = transmissionRepository.findById(transmissionId); // find transmission by id                log.info("Transmission id is " + transmissionId);

                if (transmission.isPresent()) {  // 3rd checkpoint: transmission ID

                    System.out.println("transmission ID found in DB\n");
                    log.info("Transmission id is " + transmissionId);

                    Audience audience = transmission.get().getAudience();
                    AudienceActivity audienceActivity = new AudienceActivity();

                    audienceActivity.setAudience(audience);
                    audienceActivity.setEventType(eventType);
                    audienceActivity.setAudience_email(audience.getEmail());
                    audienceActivity.setCreatedAt(LocalDateTime.now());
                    audienceActivity.setCreatedBy("SparkPost");

                    audienceActivityRepository.save(audienceActivity);
                    System.out.println("audience activity added to DB!\n");

                } else {
                    System.out.println("transmission ID not found in DB\n");

                    Response response = new Response();
                    response.setStatusCode("200");
                    response.setStatusMsg("Webhook initiated!");
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                }
            }
        }
        // Spark post requires 200 status code
        Response response = new Response(); // 直接把status code設定成200可能有問題，需要catch error等等...
        response.setStatusCode("200");
        response.setStatusMsg("Event data received!");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}