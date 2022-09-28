package springredis.demo.controller;

import org.apache.coyote.Request;
import org.json.HTTP;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import springredis.demo.Service.DAO;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.repository.*;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;
import org.springframework.http.HttpStatus;


import javax.print.attribute.standard.Media;
import java.sql.Date;
import java.util.ArrayList;
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
    private ActiveNodeRepository activeNodeRepository;
    @Autowired
    private ActiveAudienceRepository activeAudienceRepository;

    @Autowired
    private DAO dao;
    @Autowired
    RestTemplate restTemplate;

    private String str = new String(),str2=new String();

    private List<CoreModuleTask> tasks = new ArrayList<>();


    @GetMapping("/simulated_call")
    public CoreModuleTask simulated_core_module_call(){
        //现在数据库中save一个mock user，一个mock journey，一个mock node和一个mock audience
        User user = new User(); user.setShopifydevstore("example.devstore");user.setShopifyApiKey("example.key");
//        System.out.println(user.getShopifyApiKey());
        Long userid = dao.addNewUser(user).getId();
        Long journeyid = dao.addNewJourney(new Journey()).getId();
        Long nodeid = dao.addNewNode(new Node()).getId();
        Node node2=new Node(),node3 = new Node();
        node2.setType("action"); node3.setType("if/else");
        node2.setName("someName1");node3.setName("someName2");
        Long node2id = dao.addNewNode(node2).getId(), node3id = dao.addNewNode(node3).getId();
        Node node1 = dao.searchNodeById(nodeid);
        node1.setType("trigger");
        //create next nodes for node1; so when trigger happens, we can see whether task requests about node2 and node3 are posted
        node1.getNexts().add(node2id);node1.getNexts().add(node3id);
        dao.addNewNode(node1);              //update node 1
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
//restTemplate.exchange("http://localhost:8080/show2",HttpMethod.POST,new HttpEntity<String>(node1.toString()),String.class);
//restTemplate.exchange("http://localhost:8080/show2",HttpMethod.POST,new HttpEntity<String>(dao.searchNodeById(newtask.getNodeId()).toString()),String.class);
        String url = "http://localhost:8080/API_trigger";
        HttpEntity<CoreModuleTask> call = new HttpEntity<CoreModuleTask>(newtask);
        CoreModuleTask res = restTemplate.exchange(url, HttpMethod.POST,call,CoreModuleTask.class).getBody();
        return res;
    }

    @GetMapping(value="/smalltest")
    @ResponseBody
    public List<ActiveNode> test1(){
        ActiveNode actn = new ActiveNode();activeNodeRepository.save(actn).getId();
        actn.setNodeId(111L);
        ActiveAudience acta = new ActiveAudience(2L);
        ActiveAudience acta2 =new ActiveAudience(3L);
        acta.setActiveNode(actn);
        acta2.setActiveNode(actn);
        Long nid = activeNodeRepository.save(actn).getId();
        activeAudienceRepository.save(acta);
        activeAudienceRepository.save(acta2);
//        ActiveNode actn = new ActiveNode();
//        ActiveAudience acta = dao.addNewActiveAudience(new ActiveAudience());
//        actn.getActiveAudienceList().add(acta);
//        actn.setNodeId(111L);
//        actn = dao.addNewActiveNode(actn);
//        restTemplate.exchange("http://localhost:8080/show", HttpMethod.POST,new HttpEntity<>(dao.searchActiveNodeById(actn.getId())),String.class);
//        restTemplate.exchange("http://localhost:8080/show2", HttpMethod.POST,new HttpEntity<>(dao.searchActiveNodeById(actn.getId())),String.class);
//        restTemplate.exchange("http://localhost:8080/smalltest2/"+Long.toString(actn.getId()), HttpMethod.GET,new HttpEntity<>(new HttpHeaders()),String.class);
        System.out.println(nid);
        List<ActiveNode> res = restTemplate.exchange("http://localhost:8080/allActiveNodes/"+Long.toString(nid), HttpMethod.GET,new HttpEntity<>(new HttpHeaders()),List.class).getBody();
        return res;
    }

    @GetMapping(value="/smalltest2/{id}")
    public void test2(@PathVariable("id") Long ID){
        ActiveNode actn = dao.searchActiveNodeById(ID);
        restTemplate.exchange("http://localhost:8080/show2", HttpMethod.POST,new HttpEntity<>(activeNodeRepository.findByActiveNodeId(ID).toString()),String.class);
    }

    @GetMapping("/allActiveNodes/{id}")
    public ResponseEntity<List<ActiveNode>> getAllActiveNodes(@PathVariable("id") Long ID) {
        try {
            List<ActiveNode> nodes = new ArrayList<ActiveNode>();
            nodes.add(activeNodeRepository.findByActiveNodeId(ID));
            if (nodes.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(nodes, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println(e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

    @RequestMapping(value="/show2",method= RequestMethod.POST)
    public void showjson2(@RequestBody String obj){
        this.str2 = obj;
    }

    @GetMapping(value = "/show2",produces =MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String display2(){
        return this.str2;
    }

    @PostMapping("/receivetask")
    @ResponseBody
    public CoreModuleTask receiveTasks(@RequestBody CoreModuleTask task) {
        return task;
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