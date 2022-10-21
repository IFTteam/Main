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
import springredis.demo.serializer.SeDeFunction;


import javax.print.attribute.standard.Media;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
        //create a node and a corresponding active node, as well as two nodes that are its nexts. Create four audience entities, two of which has their corresponding active audience already mapped to this node, and the other two are unregistered in active DB. (simulating webhook trigger hit)
        //then, make two CMTs with node being this node (and its corresponding active node). First fill the two audiencelists with two new active audiences, then call the "createUser" api in task controller. This should create the two new active audiences1 in the active audience pool of the respective two next nodes
        //second CMT will fill the two audiencelists with the two existing active audiences(respectively), then call "moveUser" in task controller. This will result in these two audience's mapped-to active node become this node's next two nodes, respectively.
        Node node1=new Node(),node2=new Node(),node3=new Node();
        List<Long> tmp = new ArrayList<>(); tmp.add(node2.getId()); tmp.add(node3.getId());
        node1.setNexts(tmp);
        Long nid1 = dao.addNewNode(node1).getId(), nid2 = dao.addNewNode(node2).getId(), nid3= dao.addNewNode(node3).getId();
        ActiveNode act1= new ActiveNode(),act2=new ActiveNode(),act3 = new ActiveNode();
        act1.setNodeId(nid1);  act2.setNodeId(nid2); act3.setNodeId(nid3);
        act1 = dao.addNewActiveNode(act1);act2 = dao.addNewActiveNode(act2); act3 = dao.addNewActiveNode(act3);
        Long aud1id=dao.addNewAudience(new Audience()).getId(),aud2id=dao.addNewAudience(new Audience()).getId(),aud3id=dao.addNewAudience(new Audience()).getId(),aud4id=dao.addNewAudience(new Audience()).getId();
        ActiveAudience actaud1 = new ActiveAudience(), actaud2 = new ActiveAudience();
        actaud1.setAudienceId(aud1id);actaud2.setAudienceId(aud2id);
        actaud1.setActiveNode(act1); actaud2.setActiveNode(act2);;
        actaud1 = dao.addNewActiveAudience(actaud1);actaud2 = dao.addNewActiveAudience(actaud2);
        CoreModuleTask cmt1 = new CoreModuleTask(), cmt2 = new CoreModuleTask();
        cmt1.setAudienceId1();
    }

    @GetMapping(value="/smalltest")
    @ResponseBody
    public List<ActiveNode> test1(){
        ActiveNode actn = new ActiveNode();activeNodeRepository.save(actn).getId(); actn.setNodeId(111L);
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
    @GetMapping("/test/serializeNode")
    public String serializeNode() {
        SeDeFunction sede = new SeDeFunction();
        Node node1 = nodeRepository.searchNodeByid(7);
        Node node2 = nodeRepository.searchNodeByid(8);

        node1.setCreatedAt(LocalDateTime.now());
        node2.setCreatedAt(LocalDateTime.now());
        ArrayList<Node> node_list = new ArrayList<>(Arrays.asList(node1, node2));

        String nodeListString = sede.serializing(node_list);
        return nodeListString;
    }
    @GetMapping("/test/deserializeNode")
    public List<Node> deserializeNode() {
        SeDeFunction sede = new SeDeFunction();
        Node node1 = nodeRepository.searchNodeByid(7);
        Node node2 = nodeRepository.searchNodeByid(8);

        node1.setCreatedAt(LocalDateTime.now());
        node2.setCreatedAt(LocalDateTime.now());

        ArrayList<Node> node_list = new ArrayList<>(Arrays.asList(node1, node2));
        String nodeListString = sede.serializing(node_list);
        List<Node> nodeListArray = sede.deserializing(nodeListString);
        return nodeListArray;
    }


    @PostMapping("/test/addUser")
    public User addUser(User user){
        return userRepository.save(user);
    }
    //TODO: your own api
}