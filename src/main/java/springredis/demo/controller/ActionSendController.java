package springredis.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import springredis.demo.entity.Transmission;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.entity.request.Address;
import springredis.demo.entity.request.Content;
import springredis.demo.entity.request.ScheduledTransmissionRequest;
import springredis.demo.entity.request.Sender;
import springredis.demo.entity.request.TransmissionRequest;
import springredis.demo.entity.response.Response;
import springredis.demo.entity.response.SparkPostResponse;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.JourneyRepository;
import springredis.demo.repository.TransmissionRepository;
import springredis.demo.repository.UserRepository;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Slf4j // logger
@RestController
@RequestMapping(path = "/actionSend")
@CrossOrigin(origins="*")
public class ActionSendController {
    private final TransmissionRepository transmissionRepository;
    private final UserRepository userRepository;
    private final AudienceRepository audienceRepository;
    private final JourneyRepository journeyRepository;
    private final WebClient webClient;
    private final RestTemplate restTemplate = new RestTemplate();



    @Autowired
    public ActionSendController(TransmissionRepository transmissionRepository,
                                UserRepository userRepository,
                                AudienceRepository audienceRepository,
                                JourneyRepository journeyRepository,
                                WebClient webClient){
        this.transmissionRepository = transmissionRepository;
        this.userRepository = userRepository;
        this.audienceRepository = audienceRepository;
        this.journeyRepository = journeyRepository;
        this.webClient = webClient;
    }

    @SneakyThrows
    public static void main(String[] args) {
        TransmissionRequest a = new TransmissionRequest();
        a.setCampaignId("");
        a.setAddressList(Collections.singletonList(new Address().setAddress(""))); // 邮箱
        Content content = new Content();
        content.setSender(new Sender("邮箱@google.cn", "wang"));
        content.setText("这是各测试邮件的内容");
        content.setSubject("这是邮件标题");
        a.setContent(content);
        System.out.println(new ObjectMapper().writeValueAsString(a));
    }

    /**
     * <pre>
     * {@code
       curl -X POST --location "http://localhost:8080/actionSend/createTransmission" \
          -H "Content-Type: application/json" \
          -d "{
                \"campaign_id\": \"\",
                \"recipients\": [
                  {
                    \"address\": \"\"
                  }
                ],
                \"content\": {
                  \"from\": {
                    \"email\": \"ikirawang@gmail.com\",
                    \"name\": \"wang\"
                  },
                  \"subject\": \"这是邮件标题\",
                  \"html\": null,
                  \"text\": \"这是各测试邮件的内容\"
                },
                \"audience_id\": null,
                \"user_id\": null,
                \"journey_id\": null
              }"
     *
     * }
     * </pre>
     * @param transmissionRequest
     * @return
     */
    @RequestMapping(value={"/createTransmission"}, method = POST)
    @ResponseBody
    public ResponseEntity<Response> createTransmission(@RequestBody TransmissionRequest transmissionRequest){
        Optional<SparkPostResponse> sparkPostResponse = webClient.post()
                                                                 .uri("/api/v1/transmissions?num_rcpt_errors=3")
                                                                 .header("Content-Type", "application/json")
                                                                 .header("Accept", "application/json")
                                                                 .accept(MediaType.APPLICATION_JSON)
                                                                 .body(Mono.just(transmissionRequest), TransmissionRequest.class)
                                                                 .retrieve()
                                                                 .bodyToMono(SparkPostResponse.class)
                                                                 .blockOptional();

        if(!sparkPostResponse.isPresent()) return ResponseEntity
                .status(HttpStatus.FAILED_DEPENDENCY).body(new Response());

        //record keeping
        Transmission transmission = new Transmission();
        log.info("transmission id is" + sparkPostResponse.get().getSparkPostResults().getTransmissionId());
        transmission.setId(sparkPostResponse.get().getSparkPostResults().getTransmissionId());
        transmission.setAudience_email(transmissionRequest.getAddressList().get(0).getAddress());
        transmission.setAudience(audienceRepository.getReferenceById(transmissionRequest.getAudienceId()));
        transmission.setUser(userRepository.getReferenceById(transmissionRequest.getUserId()));
        transmission.setCreatedAt(LocalDateTime.now());
        transmission.setCreatedBy("" + transmissionRequest.getUserId());
        transmission.setJourney(journeyRepository.findById(transmissionRequest.getJourneyId()).get());
        transmissionRepository.save(transmission);

        Response response = new Response();
        response.setStatusCode(200);
        response.setMsg("Transmission successfully created");

        //return task to core module
        BaseTaskEntity coreModuleTask = new BaseTaskEntity();
        restTemplate.postForObject("http://localhost:8081/ReturnTask", coreModuleTask, String.class);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @RequestMapping(value={"/createScheduledTransmission"}, method = POST)
    @ResponseBody
    public ResponseEntity<Response> createScheduleTransmission(
                                @RequestBody ScheduledTransmissionRequest scheduledTransmissionRequest){
        Optional<SparkPostResponse> sparkPostResponse = webClient.post()
                .uri("/api/v1/transmissions?num_rcpt_errors=3")
//                .header("Content-Type", "application/json")
//                .header("Accept", "application/json")
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(scheduledTransmissionRequest), ScheduledTransmissionRequest.class)
                .retrieve()
                .bodyToMono(SparkPostResponse.class)
                .blockOptional();

        if(!sparkPostResponse.isPresent()) return ResponseEntity
                .status(HttpStatus.FAILED_DEPENDENCY).body(new Response());

        //record keeping
        Transmission transmission = new Transmission();
        log.info("transmission id is" + sparkPostResponse.get().getSparkPostResults().getTransmissionId());
        transmission.setId(sparkPostResponse.get().getSparkPostResults().getTransmissionId());
        transmission.setAudience_email(scheduledTransmissionRequest.getAddressList().get(0).getAddress());
        transmission.setAudience(audienceRepository.getReferenceById(scheduledTransmissionRequest.getAudienceId()));
        transmission.setUser(userRepository.getReferenceById(scheduledTransmissionRequest.getUserId()));
        transmission.setCreatedAt(LocalDateTime.now());
        transmission.setCreatedBy("" + scheduledTransmissionRequest.getUserId());
        transmission.setJourney(journeyRepository.findById(scheduledTransmissionRequest.getJourneyId()).get());
        transmissionRepository.save(transmission);

        Response response = new Response();
        response.setStatusCode(200);
        response.setMsg("Scheduled Transmission successfully created");

        //return task to core module
        BaseTaskEntity coreModuleTask = new BaseTaskEntity();
        restTemplate.postForObject("http://localhost:8081/ReturnTask", coreModuleTask, String.class);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}

