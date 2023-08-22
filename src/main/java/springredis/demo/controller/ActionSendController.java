package springredis.demo.controller;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.entity.request.*;
import springredis.demo.entity.response.Response;
import springredis.demo.entity.response.SparkPostResponse;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.JourneyRepository;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.TransmissionRepository;
import springredis.demo.repository.UserRepository;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Slf4j
@RestController
@RequestMapping(path = "/actionSend")
@CrossOrigin(origins="*")
public class ActionSendController {
    private final TransmissionRepository transmissionRepository;
    private final UserRepository userRepository;
    private final AudienceRepository audienceRepository;
    private final ActiveAudienceRepository activeAudienceRepository;
    private final NodeRepository nodeRepository;
    private final JourneyRepository journeyRepository;
    private final WebClient webClient;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String unsubscribe_url = "https://www.yelp.com/";

    @Autowired
    public ActionSendController(TransmissionRepository transmissionRepository,
                                UserRepository userRepository,
                                AudienceRepository audienceRepository,
                                ActiveAudienceRepository activeAudienceRepository,
                                NodeRepository nodeRepository,
                                JourneyRepository journeyRepository,
                                WebClient webClient){
        this.transmissionRepository = transmissionRepository;
        this.userRepository = userRepository;
        this.audienceRepository = audienceRepository;
        this.activeAudienceRepository = activeAudienceRepository;
        this.nodeRepository = nodeRepository;
        this.journeyRepository = journeyRepository;
        this.webClient = webClient;
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
    //@RequestMapping(value={"/createTransmission"}, method = POST)
    //@ResponseBody
    public ResponseEntity<Response> createTransmission(TransmissionRequest transmissionRequest){


        HashMap<Object, Object> param = new HashMap<>();
        param.put("options", new HashMap<String, Object>() {{
            put("open_tracking", true);
            put("click_tracking", true);
        }});
        param.put("metadata", new HashMap<String, Object>() {{
            put("user_type", "students");
            put("education_level", "college");
        }});

        Optional<SparkPostResponse> sparkPostResponse = webClient.post()
                .uri("/api/v1/transmissions?num_rcpt_errors=3")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(transmissionRequest), TransmissionRequest.class)
                .retrieve()
                .bodyToMono(SparkPostResponse.class)
                .blockOptional();
        if(!sparkPostResponse.isPresent()) 
        {
        	return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(new Response());
        }
        System.out.println("=============================================SparkPost toString: " + sparkPostResponse.get().toString());
        System.out.println("=============================================SparkPost Transmission ID: " + sparkPostResponse.get().getSparkPostResults().getTransmissionId());
        System.out.println("=============================================SparkPost Total Accepted Recipients: " + sparkPostResponse.get().getSparkPostResults().getTotalAcceptedRecipients());
        System.out.println("=============================================SparkPost Total Rejected Recipients: " + sparkPostResponse.get().getSparkPostResults().getTotalRejectedRecipients());
        System.out.println("=============================================SparkPost Ends");

        //record keeping
        Transmission transmission = new Transmission();
        log.info("transmission id is" + sparkPostResponse.get().getSparkPostResults().getTransmissionId());
        transmission.setId(sparkPostResponse.get().getSparkPostResults().getTransmissionId());
        transmission.setEmail(transmissionRequest.getAddressList().get(0).getAddress());
        transmission.setAudience(audienceRepository.getReferenceById(transmissionRequest.getAudienceId()));
        System.out.println(transmissionRequest.getUserId());
        transmission.setUser(userRepository.getReferenceById(transmissionRequest.getUserId()));
        transmission.setCreatedAt(LocalDateTime.now());
        transmission.setCreatedBy("" + transmissionRequest.getUserId());
        transmission.setJourney(journeyRepository.searchJourneyById(transmissionRequest.getJourneyId()));
        System.out.println("Front end ID: " + journeyRepository.searchJourneyById(transmissionRequest.getJourneyId()).getFrontEndId());
        transmissionRepository.save(transmission);

        Response response = new Response();
        response.setStatusCode(200);
        response.setMsg("Transmission successfully created");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @RequestMapping(value={"/createScheduledTransmission"}, method = POST)
    @ResponseBody
    public ResponseEntity<Response> createScheduleTransmission(
                                @RequestBody ScheduledTransmissionRequest scheduledTransmissionRequest){
        Optional<SparkPostResponse> sparkPostResponse = webClient.post()
                .uri("/api/v1/transmissions?num_rcpt_errors=3")
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
        transmission.setEmail(scheduledTransmissionRequest.getAddressList().get(0).getAddress());
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
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @RequestMapping(value={"/createCMTTransmission"}, method = POST)
    @ResponseBody
    public CoreModuleTask createCMTTransmission(@RequestBody CoreModuleTask coreModuleTask) {
    	List<ActiveAudience> activeAudienceList = new ArrayList<ActiveAudience>();  //obtain active audience list from CMT
    	for(int i = 0; i < coreModuleTask.getActiveAudienceId1().size(); i++)
    	{
    		Optional<ActiveAudience> activeAudience = activeAudienceRepository.findById(coreModuleTask.getActiveAudienceId1().get(i));
    		if(activeAudience.isPresent())
    		{
    			activeAudienceList.add(activeAudience.get());
    		}
    	}

    	List<Audience> audienceList = new ArrayList<Audience>();  //obtain audience list from the CMT
    	for(int i = 0; i < activeAudienceList.size(); i++)
    	{
    		Optional<Audience> audience = audienceRepository.findById(activeAudienceList.get(i).getAudienceId());
    		if(audience.isPresent() == true)
    		{
    			audienceList.add(audience.get());
    		}
    	}
    	
    	TransmissionRequest request = new TransmissionRequest();
    	
    	request.setCampaignId("1");  //Not entirely sure how to obtain campaign ID yet

    	List<Address> addressList = new ArrayList<Address>();  //set address list for the request
    	for(int i = 0; i < audienceList.size(); i++) 
    	{
    		Address address = new Address();
    		address.setAddress(audienceList.get(i).getEmail());
    		addressList.add(address);
    	}
    	request.setAddressList(addressList);

        System.out.println(addressList.toString());

    	Content content = new Content();  //set content for the request
    	Node node = nodeRepository.findById(coreModuleTask.getNodeId()).get();

        String properties = node.getProperties();
        JSONObject jsonObject = new JSONObject(properties);


        Sender sender = new Sender();
    	//(sender, subject, email, name"sender", subject, html, text)
        String user_name = "user1";
        String add = user_name + "@paradx.dev";
    	sender.setEmail("duke.tang@paradx.dev");
        if(jsonObject.isNull("sender")) {
            sender.setName("Unknow");
        }else{
            sender.setName(jsonObject.getString("sender"));
        }

    	content.setSender(sender);
        if(jsonObject.isNull("subject"))
        {
            content.setSubject("Unknow");
        }else{
            content.setSubject(jsonObject.getString("subject"));
        }
        Options options = new Options();
        options.setOpenTracking(true);
        options.setClickTracking(true);
        content.setType(jsonObject.getString("type"));
        content.setContent(jsonObject.getString("content"));
        if (content.getType().equalsIgnoreCase("html")) {
            content.setHtml(content.getContent(), unsubscribe_url);
            content.setText("text here");
        }
        if (content.getType().equalsIgnoreCase("text")) {
            content.setText(content.getContent(), unsubscribe_url);
        }
        request.setContent(content);
        request.setOptions(options);
        if(jsonObject.getString("content") != null)
            content.setText(jsonObject.getString("content"));
        else
    	    content.setText("hello,testing");
    	request.setContent(content);

    	request.setAudienceId(coreModuleTask.getAudienceId1().get(0));  //set audience id. Is it just the first audience id stored on CMT?

        System.out.println("The coreModuleTask is:" + coreModuleTask.toString());

        /*
        // TODO: user is manually created generated here for testing purpose
        */

        // Find its user from journey
        Journey journey = journeyRepository.searchJourneyById(coreModuleTask.getJourneyId());
        System.out.println("(ActionSendController) The CMT module journey is createdBy " + journey.getCreatedBy());
        Long userId = Long.valueOf(journey.getCreatedBy());
        System.out.println("(ActionSendController) The CMT module userId is " + userId);
        coreModuleTask.setUserId(userId);
        System.out.println("The coreModuleTask UserId is:" + coreModuleTask.getUserId());
    	request.setUserId(coreModuleTask.getUserId());  //set user id
    	request.setJourneyId(coreModuleTask.getJourneyId());  //set journey id
    	//Ask JiaQi what needs to be done before returning the CMT
        ResponseEntity<Response> response = createTransmission(request);
        System.out.println(response);
    	return coreModuleTask;
    }
    
    //Testing
    //Ask JiaQi what properties to set in CoreModuleTask to be returned after it completes
    @GetMapping("/test/addTransmission")
    public TransmissionRequest createTransmission() {
    	TransmissionRequest request = new TransmissionRequest();
    	Address address = new Address();
    	address.setAddress("ikirawang@gmail.com");
    	List<Address> list = new ArrayList<>();
    	list.add(address);
    	request.setCampaignId("1");
    	request.setAddressList(list);
    	Content content = new Content();
    	Sender sender = new Sender();
    	sender.setEmail("testing@paradx.dev");
    	sender.setName("Luke Leon");
    	content.setSender(sender);
    	content.setSubject("News1");
    	content.setHtml(" ");
    	content.setText("Piggy ZHu Mi Bun");
    	request.setContent(content);
    	request.setAudienceId((long)1);
    	request.setJourneyId((long)4);
    	request.setCampaignId("1");
    	request.setUserId((long)5);
    	restTemplate.postForObject("http://localhost:8081/actionSend/createTransmission", request, String.class);
    	return request;
    }

    @GetMapping("/test/addCMTTransmission")
    public CoreModuleTask addCMTTransmission() {
        CoreModuleTask coreModuleTask = new CoreModuleTask();
        coreModuleTask.getActiveAudienceId1().add((long)2);
        coreModuleTask.setNodeId((long)8);
        List<Long> list = new ArrayList<Long>();
        list.add((long)1);
        coreModuleTask.setAudienceId1(list);
        coreModuleTask.setJourneyId((long)4);
        coreModuleTask.setUserId((long)5);
        //coreModuleTask

        Optional<ActiveAudience> activeAudience = activeAudienceRepository.findById(coreModuleTask.getActiveAudienceId1().get(0));
        System.out.println("ActiveAudience=======================" + activeAudience.isPresent());
        Optional<Audience> audience = audienceRepository.findById(activeAudience.get().getAudienceId());
        System.out.println("Audience=======================" + audience.isPresent());
        Optional<Node> node = nodeRepository.findById(coreModuleTask.getNodeId());
        System.out.println("Node=======================" + node.isPresent());
        restTemplate.postForObject("http://localhost:8080/actionSend/createCMTTransmission", coreModuleTask, String.class);
        return coreModuleTask;
    }

    @GetMapping("/test/addDummyData")
    public void addDummyData() {
        Audience audience = new Audience();
        audience.setFirstName("Brandon");
        audience.setLastName("Bao");
        audience.setEmail("ikirawang@gmail.com");
        audienceRepository.save(audience);
        ActiveAudience activeAudience = new ActiveAudience();
        activeAudience.setAudienceId(audience.getId());
        activeAudienceRepository.save(activeAudience);
        Node node = new Node();
        node.setType("Luke Leon,News1,testing@paradx.dev,Luke Leon,News1, ,Piggy Zhu Mi Bun"); //should be send - subject - content and should be node property
        node.setProperties("Luke Leon,News1,testing@paradx.dev,Luke Leon,News1, ,Piggy Zhu Mi Bun");
        nodeRepository.save(node);
        Journey journey = new Journey();
        journeyRepository.save(journey);
        User user = new User();
        userRepository.save(user);
    }
}