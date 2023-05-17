package springredis.demo.controller;

import lombok.extern.slf4j.Slf4j;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import springredis.demo.Service.DAO;
import springredis.demo.entity.*;
import springredis.demo.repository.*;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

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
    public ResponseEntity<Response> handleEventWebhook(HttpEntity<String> httpEntity) {

        try {
            String payload = httpEntity.getBody();
            log.info("Received event payload from SparkPost");
            JSONArray events = new JSONArray(payload);
            Response response = new Response();

            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                JSONObject msysObject = event.optJSONObject("msys");

                if (msysObject != null && msysObject.has("track_event")) {
                    JSONObject category = msysObject.optJSONObject("track_event");
                    Long transmissionId = category.optLong("transmission_id");
                    String eventType = category.getString("type");
                    String audienceEmail = category.getString("rcpt_to");

                    if (eventType.equals("open") || eventType.equals("click")) {

                        Optional<Transmission> transmission = transmissionRepository.findById(transmissionId);

                        if (transmission.isEmpty()) {
                            response.setStatusCode("200");
                            response.setStatusMsg("Webhook initiated!");
                            return ResponseEntity.status(HttpStatus.OK).body(response);

                        } else if (audienceActivityRepository
                                .countDistinctEventTypeByTransmissionIdAndAudienceEmail(transmissionId, audienceEmail) == 2) {

                            response.setStatusCode("200");
                            response.setStatusMsg("Duplicate data!");
                            return ResponseEntity.status(HttpStatus.OK).body(response);

                        } else {
                            String existingEventType = audienceActivityRepository
                                    .getEventTypeByTransmissionIdAndAudienceEmail(transmissionId, audienceEmail);

                            if (existingEventType == null || !existingEventType.equals(eventType)) {
                                saveAudienceActivity(transmissionId, eventType, transmission.get(), audienceEmail);

                                response.setStatusCode("200");
                                response.setStatusMsg("Data added to database!");
                                return ResponseEntity.status(HttpStatus.OK).body(response);
                            }
                        }
                    }
                }
            }
            response.setStatusCode("200");
            response.setStatusMsg("Event data received!");
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (JSONException | NoSuchElementException | IllegalArgumentException e) {
            Response response = new Response();
            response.setStatusCode("500");
            response.setStatusMsg(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private void saveAudienceActivity(Long transmissionId, String eventType, Transmission transmission, String audienceEmail) {

        Audience audience = transmission.getAudience();

        AudienceActivity audienceActivity = new AudienceActivity();
        audienceActivity.setAudience(audience); //audience_id
        audienceActivity.setEventType(eventType);
        audienceActivity.setAudience_email(audienceEmail);
        audienceActivity.setCreatedAt(LocalDateTime.now());
        audienceActivity.setCreatedBy("SparkPost");
        audienceActivity.setTransmission_id(transmissionId);
        audienceActivityRepository.save(audienceActivity);
    }

    @RequestMapping(value = "/sparkpost_create_webhook", method = POST)
    public ResponseEntity<Response> createSparkpostWebhook() {
        try {
            String payload = generateWebhookPayload();
            WebClient client = WebClient.create("https://api.sparkpost.com/api/v1/webhooks");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth("358294aeb167a63aa0ade3a287ef013559e3d964");

            ResponseEntity<String> responseEntity = client.post()
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .bodyValue(payload)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            assert responseEntity != null;
            HttpStatus statusCode = responseEntity.getStatusCode();
            String responseBody = responseEntity.getBody();

            if (statusCode == HttpStatus.OK) {
                Response successResponse = new Response("Webhook successfully created", "200");
                return ResponseEntity.status(HttpStatus.OK).body(successResponse);
            } else if (statusCode == HttpStatus.CONFLICT) {
                Response conflictResponse = new Response("Webhook already exists: " + responseBody, "409");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResponse);
            } else {
                Response errorResponse = new Response("Failed to create webhook: " + responseBody, statusCode.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        } catch (Exception e) {
            Response errorResponse = new Response("Failed to create webhook: " + e.getMessage(), "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private String generateWebhookPayload() {
        JSONObject payload = new JSONObject();
        payload.put("name", "Webhook one"); // Name of webhook
        payload.put("target", "https://9cdf-104-244-243-145.ngrok-free.app" +// Set ngrok url here
                "/analytics/webhook/eventWebhook");

        JSONArray events = new JSONArray();
        events.put("open");
        events.put("click");
        payload.put("events", events);

        return payload.toString();
    }

    // create the webhook here
    public static void main(String[] args) {  // Execute after running backend: DemoApplication
        try {
            // Specify the URL for the POST request
            URL url = new URL("https://9cdf-104-244-243-145.ngrok-free.app" + // Set ngrok url here
                    "/analytics/webhook/sparkpost_create_webhook");

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}