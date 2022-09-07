package springredis.demo.controller;

import org.apache.coyote.Request;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import springredis.demo.Service.DAO;
import springredis.demo.entity.*;
import springredis.demo.repository.*;

import java.sql.Date;
import java.util.List;

@RestController
public class TestController {
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JourneyRepository journeyRepository;
    @Autowired
    private AudienceRepository audienceRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private DAO dao;
    @Autowired
    RestTemplate restTemplate;

    private String str = new String();


    @GetMapping("/simulated_call")
    public CoreModuleTask simulated_core_module_call(){
        //现在数据库中save一个mock user，一个mock journey，一个mock node和一个mock audience
        User user = new User(); user.setShopifydevstore("example.devstore");user.setShopifyApiKey("example.key");
//        System.out.println(user.getShopifyApiKey());
        Long userid = dao.addNewUser(user).getId();
        Long journeyid = dao.addNewJourney(new Journey()).getId();
        Long nodeid = dao.addNewNode(new Node()).getId();
        Audience audience = new Audience(); audience.setEmail("example@gmail.com");
        Long audienceid = dao.addNewAudience(audience).getId();
        CoreModuleTask newtask = new CoreModuleTask();
        newtask.setType("API Trigger");
        newtask.setName("shopify_create_trigger");
        //其他coremoduletask的参数没有设置，因为在trigger api中不需要。这里单独测试trigger api的功能。
        newtask.setUserId(userid);
        newtask.setAudienceId(audienceid);
        newtask.setJourneyId(journeyid);
        newtask.setNodeId(nodeid);
        String url = "http://localhost:8080/API_trigger";
        HttpEntity<CoreModuleTask> call = new HttpEntity<CoreModuleTask>(newtask);
        CoreModuleTask res = restTemplate.exchange(url, HttpMethod.POST,call,CoreModuleTask.class).getBody();
        return res;
    }

    @RequestMapping(value="/show",method= RequestMethod.POST)
    public void showjson(@RequestBody String obj){
        this.str = obj;
    }

    @GetMapping(value = "/show",produces =MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String display(){
        return this.str;
    }



    //这里我随便测了几个应该都是可以用的，注意有些参数是必须要有的
    @GetMapping("/test/getAllNode")
    public List<Node> getAllNode(){
        return nodeRepository.findAll();
    }

    @PostMapping("/test/addNode")
    public Node addNode(Node node){

        return nodeRepository.save(node);
    }

    @GetMapping("/test/getAllUser")
    public List<User> getAllUser(){
        return userRepository.findAll();
    }

    //这里展示了一下如果想用与变量名不同的Key的传参方法，就要列出所有的属性，很是麻烦，建议直接使用下面那种传参
//    @PostMapping("/test/addUser")
//    public User addUser(
//            @RequestParam("domain") String domain,
//            @RequestParam("avatarUrl") String avatarUrl,
//            @RequestParam("companyId") String companyId,
//            @RequestParam("unsubscribeLink") String unsubscribeLink,
//            @RequestParam("subscriptionType") String subscriptionType,
//
//            @RequestParam("lastModifiedBy") String lastModifiedBy,
//            @RequestParam("contactName") String contactName,
//            @RequestParam("contactEmail") String contactEmail,
//            @RequestParam("contactPhone") String contactPhone,
//            @RequestParam("address") String address,
//            @RequestParam("api_id") Long api_id,
//            @RequestParam("preferEmailSvcProvider") Integer preferEmailSvcProvider,
//            @RequestParam("onlySendDeliverableEmail") Integer onlySendDeliverableEmail,
//            @RequestParam("unsubscribeType") Integer unsubscribeType,
//            @RequestParam("salesforceApiKey") String salesforceApiKey,
//            @RequestParam("hubspotApiKey") String hubspotApiKey,
//            @RequestParam("shopifyApiKey") String shopifyApiKey,
//            @RequestParam("facebookAdsApiKey") String facebookAdsApiKey){
//
//        User user = new User(domain,avatarUrl,domain,companyId,unsubscribeLink,subscriptionType,null,lastModifiedBy,null,contactName,contactEmail,contactPhone,address,api_id,preferEmailSvcProvider,onlySendDeliverableEmail,unsubscribeType,salesforceApiKey,hubspotApiKey,shopifyApiKey,facebookAdsApiKey);
//        return userRepository.save(user);
//    }

    @PostMapping("/test/addUser")
    public User addUser(User user){
        return userRepository.save(user);
    }
    //TODO: your own api
}