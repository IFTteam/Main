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

import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import springredis.demo.Service.DAO;
import springredis.demo.entity.*;
import springredis.demo.repository.*;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
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
            List<JSONObject> msysObjects = new ArrayList<>();
            Response response = new Response();

            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                JSONObject msysObject = event.getJSONObject("msys");
                msysObjects.add(msysObject);

                if (msysObject != null && msysObject.has("track_event")) {
                    JSONObject category = msysObject.optJSONObject("track_event");
                    Long transmissionId = category.optLong("transmission_id");
                    String eventType = category.getString("type");
                    String audienceEmail = category.getString("rcpt_to");
                    String targetLinkUrl = category.optString("target_link_url");

                    if (eventType.equals("open") || eventType.equals("click")) {
                        System.out.println(eventType);
                        Optional<Transmission> transmission = transmissionRepository.findById(transmissionId);
//                        System.out.println("a check point");

                        //                            System.out.println(transmissionId); // 7236383903592918383
                        //                            System.out.println(eventType); // click
                        //                            System.out.println(targetLinkUrl); // https://www.cnn.com/
                        //                            System.out.println(transmission.get()); // BaseEntity(createdAt=2023-05-23T14:14:50.407479, createdBy=1, updatedAt=null, updatedBy=null)
                        //                            System.out.println(audienceEmail); // hhuang60@hawk.iit.edu
                        // error: query did not return a unique result: 2 (solved)
                        //                            System.out.println("d check point");
                        //                            response.setStatusCode("200");
                        //                            response.setStatusMsg("Data added to database!");
                        //                            return ResponseEntity.status(HttpStatus.OK).body(response);
                        transmission.ifPresent(value -> saveAudienceActivity(transmissionId, eventType, targetLinkUrl, value, audienceEmail));
                    }
                }
            }
//            System.out.println("e check point");
            response.setStatusCode("200");
            response.setStatusMsg("Event data received!");
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (JSONException | NoSuchElementException | IllegalArgumentException e) {
            System.out.println("f check point");
            Response response = new Response();
            response.setStatusCode("500");
            response.setStatusMsg(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    //error: query did not return a unique result: 2 (solved)
    private void saveAudienceActivity(Long transmissionId, String eventType, String targetLinkUrl, Transmission transmission, String audienceEmail) {
        // Check if the event type already exists for the given transmission ID and audience email
//        System.out.println("b check point: " + transmissionId + " and this: " + audienceEmail + " also this: " + targetLinkUrl);
        int numberOfExistingEvenType = audienceActivityRepository.countDistinctEventTypeByTransmissionIdAndAudienceEmail(transmissionId, audienceEmail);
//        System.out.println("b1 for one apple: " + numberOfExistingEvenType); // 2
        List<String> existingEvenType = audienceActivityRepository.getEventTypeByTransmissionIdAndAudienceEmail(transmissionId, audienceEmail);
//        System.out.println("b2 for two apple: " + existingEvenType); // [click, open]
        List<String> existingUrl = audienceActivityRepository.getLinkUrlByEventTypeAndTransmissionIdAndAudienceEmail(eventType, transmissionId, audienceEmail);
        // error: query did not return a unique result: 2 (solved)
//        System.out.println("b3 for three apple: " + existingUrl); // https://www.bbc.com/news

        if ((numberOfExistingEvenType == 2 && eventType.equals("open")) || // 數據庫裡有2種事件，open和click都有；payload為open，不存
                (numberOfExistingEvenType == 2 && existingUrl.contains(targetLinkUrl)) || // 數據庫裡有2種事件，open和click都有；payload為click，但已有相同的URL存在數據庫裡，不存
                (existingEvenType.contains("open") && eventType.equals("open"))) { // 數據庫裡只有1種事件 為open；payload為open，不存
            return;
        }
//        System.out.println("c check point");
        Audience audience = transmission.getAudience();

        AudienceActivity audienceActivity = new AudienceActivity();

        audienceActivity.setEventType(eventType);
        audienceActivity.setAudience_email(audienceEmail); // audienceEmail: payload裡的email

        String audienceEmailFromAudienceActivityTable = audienceActivity.getAudience_email();
        if (audienceEmailFromAudienceActivityTable.equals(audienceEmail)) {
            audienceActivity.setAudience(audienceRepository.findByEmail(audienceEmail));
        } else {
            audienceActivity.setAudience(audience); // audience_id //
        }
        audienceActivity.setCreatedAt(LocalDateTime.now());
        audienceActivity.setCreatedBy("SparkPost");
        audienceActivity.setTransmission_id(transmissionId);
        audienceActivity.setLink_url(targetLinkUrl);

        audienceActivityRepository.save(audienceActivity);
    }

    private final String targetUrl = "https://15ba-104-244-243-145.ngrok-free.app/analytics/webhook/eventWebhook"; // set your target URL here
    // https://9cdf-104-244-243-145.ngrok-free.app/analytics/webhook/sparkpost_create_webhook <- this is the url for POST request: create a webhook

    private JSONObject generateWebhookPayload() { // name, target URL, and event type are required for creating a webhook
        JSONObject payload = new JSONObject();
        payload.put("name", "Webhook one"); // (Optional) Name of webhook
        payload.put("target", targetUrl); // set target URL

        JSONArray events = new JSONArray(); // set event type
        events.put("delivery");
        events.put("injection");
        events.put("open");
        events.put("click");
        payload.put("events", events);

        return payload;
    }

    @RequestMapping(value = "/sparkpost_create_webhook", method = POST)
    public ResponseEntity<Response> createSparkpostWebhook() {
        try {
            JSONObject payload = generateWebhookPayload();
            WebClient client = WebClient.create("https://api.sparkpost.com/api/v1/webhooks");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth("358294aeb167a63aa0ade3a287ef013559e3d964");

            // Retrieve existing webhooks
            ResponseEntity<String> existingWebhooksResponse = client.get()
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            assert existingWebhooksResponse != null;
            HttpStatus existingWebhooksStatusCode = existingWebhooksResponse.getStatusCode();
            String existingWebhooksResponseBody = existingWebhooksResponse.getBody();

            if (existingWebhooksStatusCode == HttpStatus.OK) {
                // Process the response body to check for existing webhooks
                JSONObject json = new JSONObject(existingWebhooksResponseBody);
                JSONArray results = json.getJSONArray("results");

                for (int i = 0; i < results.length(); i++) {
                    JSONObject webhook = results.getJSONObject(i);
                    String existingTarget = webhook.getString("target");

                    // Check if target URL and event types match
                    if (existingTarget.equals(targetUrl)) { // target URL
                        JSONArray existingEvents = webhook.getJSONArray("events");
                        JSONArray newEvents = payload.getJSONArray("events");

                        // Compare event types
                        if (existingEvents.toString().equals(newEvents.toString())) {
                            // Existing webhook found with the same target URL and event types
                            Response conflictResponse = new Response("Webhook already exists with the same target URL and event types.", "409");
                            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResponse);
                        }
                        Response conflictResponse = new Response("Webhook already exists with the same target URL.", "409");
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResponse);
                    }
                }
            } else {
                Response errorResponse = new Response("Failed to retrieve existing webhooks: " + existingWebhooksResponseBody, existingWebhooksStatusCode.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            // Create the new webhook if no matching webhook was found
            ResponseEntity<String> responseEntity = client.post()
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .bodyValue(payload.toString())
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            assert responseEntity != null;
            HttpStatus statusCode = responseEntity.getStatusCode();
            String responseBody = responseEntity.getBody();

            if (statusCode == HttpStatus.OK) {
                Response successResponse = new Response("Webhook successfully created", "200");
                return ResponseEntity.status(HttpStatus.OK).body(successResponse);
            } else {
                Response errorResponse = new Response("Failed to create webhook: " + responseBody, statusCode.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        } catch (Exception e) {
            Response errorResponse = new Response("Failed to create webhook: " + e.getMessage(), "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

//     create the webhook here
    public static void main(String[] args) {  // Execute after running backend: DemoApplication
        try {
            // Specify the URL for the POST request
            URL url = new URL("https://15ba-104-244-243-145.ngrok-free.app/analytics/webhook/sparkpost_create_webhook");

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

    @RequestMapping(value = "/sparkpost_get_webhookID", method = GET)
    public ResponseEntity<Response> getSparkpostWebhookID() {
        try {
            WebClient client = WebClient.create("https://api.sparkpost.com/api/v1/webhooks");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth("358294aeb167a63aa0ade3a287ef013559e3d964");

            ResponseEntity<String> responseEntity = client.get()
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            assert responseEntity != null;
            HttpStatus statusCode = responseEntity.getStatusCode();
            String responseBody = responseEntity.getBody();

            if (statusCode == HttpStatus.OK) {
                // Process the response body to extract webhook IDs
                JSONObject json = new JSONObject(responseBody);
                JSONArray results = json.getJSONArray("results");

                List<String> webhookIds = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject webhook = results.getJSONObject(i);
                    String webhookId = webhook.getString("id");
                    webhookIds.add(webhookId);
                }

                // Return the webhook IDs in the response
                Response successResponse = new Response("Webhook IDs: " + webhookIds.toString(), "200");
                return ResponseEntity.status(HttpStatus.OK).body(successResponse);
            }
            else {
                Response errorResponse = new Response("Failed to retrieve webhook: " + responseBody, statusCode.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        } catch (Exception e) {
            Response errorResponse = new Response("Failed to retrieve webhook: " + e.getMessage(), "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


}