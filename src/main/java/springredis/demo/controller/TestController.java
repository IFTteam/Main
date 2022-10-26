package springredis.demo.controller;

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


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    @GetMapping("/test/serializeNode")
    public String serializeNode() {
        SeDeFunction sede = new SeDeFunction();
        Node node1 = nodeRepository.searchNodeByid(1);
        Node node2 = nodeRepository.searchNodeByid(2);
        Node node3 = nodeRepository.searchNodeByid(3);
        Node node4 = nodeRepository.searchNodeByid(4);

        ArrayList<Node> node_list = new ArrayList<>(Arrays.asList(node1, node2, node3, node4));

        String nodeListString = sede.serializing(node_list);
        return nodeListString;
    }
    @GetMapping("/test/deserializeNode")
    public List<Node> deserializeNode() {
        SeDeFunction sede = new SeDeFunction();
        Node node1 = nodeRepository.searchNodeByid(1);
        Node node2 = nodeRepository.searchNodeByid(2);
        Node node3 = nodeRepository.searchNodeByid(3);
        Node node4 = nodeRepository.searchNodeByid(4);


        ArrayList<Node> node_list = new ArrayList<>(Arrays.asList(node1, node2, node3, node4));
        String nodeListString = sede.serializing(node_list);
        List<Node> nodeListArray = sede.deserializing(nodeListString);
        return nodeListArray;
    }

    @GetMapping("/test/addTestNodes")
    public void addTestNodes() {
        Node node1 = new Node();
        Node node2 = new Node();
        Node node3 = new Node();
        Node node4 = new Node();

        node1.setType("trigger");
        node2.setType("action");
        node2.setName("someName1");
        node3.setType("if/else");
        node3.setName("someName2");
        node1.setCreatedAt(LocalDateTime.now());

        Long node1id = dao.addNewNode(node1).getId();
        Long node2id = dao.addNewNode(node2).getId();
        Long node3id = dao.addNewNode(node3).getId();
        Long node4id = dao.addNewNode(node4).getId();

        List<Long> onesNexts = new ArrayList<>();
        onesNexts.add(node2id);
        List<Long> twosNexts = new ArrayList<>();
        twosNexts.add(node3id);
        twosNexts.add(node4id);
        node1.setHeadOrTail(1);
        node2.setHeadOrTail(0);
        node3.setHeadOrTail(2);
        node4.setHeadOrTail(2);
        List<Long> twosLasts = new ArrayList<>();
        twosLasts.add(node1id);
        List<Long> threesLasts = new ArrayList<>();
        threesLasts.add(node2id);
        List<Long> foursLasts = new ArrayList<>();
        foursLasts.add(node2id);
        node2.setLasts(twosLasts);
        node3.setLasts(threesLasts);
        node4.setLasts(foursLasts);

        node1.setNexts(onesNexts);
        node2.setNexts(twosNexts);

        dao.addNewNode(node1);
        dao.addNewNode(node2);
        dao.addNewNode(node3);
        dao.addNewNode(node4);
    }

    private Node createEndNode() {
        Node endNode = new Node();
        endNode.setType("end");
        endNode.setHeadOrTail(2);
        endNode.setName("endNode");
        return endNode;
    }
    private Node createNodeFromNodeJsonModel(NodeJsonModel nodeJsonModel) {
        Node newNode = new Node();
        LocalDateTime createAt = LocalDateTime.parse(nodeJsonModel.getCreatedAt(), DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime updateAt = LocalDateTime.parse(nodeJsonModel.getUpdatedAt(), DateTimeFormatter.ISO_DATE_TIME);

        newNode.setFrontEndId(nodeJsonModel.getId());
        newNode.setUpdatedBy(nodeJsonModel.getUpdatedBy());
        newNode.setUpdatedAt(updateAt);
        newNode.setType(nodeJsonModel.getComponentType());
        newNode.setHeadOrTail(0);
        newNode.setCreatedBy(nodeJsonModel.getCreatedBy());
        newNode.setCreatedAt(createAt);
        newNode.setName(nodeJsonModel.getName());
        return newNode;
    }
    public Long dfs(NodeJsonModel[] nodeJsonModelList, int idx) {
        Node newNode = createNodeFromNodeJsonModel(nodeJsonModelList[idx]);
        // We need to store the node in DB first
        dao.addNewNode(newNode);
        // so that we can get the node's id
        Long nodeId = newNode.getId();
        nodeIdList.add(newNode.getId());
        newNode = dao.searchNodeById(nodeId);

        List<Long> nexts = new ArrayList<>();
        // If it is an if/else node. It'll have two next nodes.
        if (newNode.getType().equals("switch")) {
             Long child1 = null;
             Long child2 = null;
             if (nodeJsonModelList[idx].getBranches().getTrue().length != 0) {
                 child1 = dfs(nodeJsonModelList[idx].getBranches().getTrue(), 0);
             }
             if (nodeJsonModelList[idx].getBranches().getFalse().length != 0) {
                 child2 = dfs(nodeJsonModelList[idx].getBranches().getFalse(), 0);
             }
             if (child1 == null) {
                 Node endNode = createEndNode();
                 child1 = dao.addNewNode(endNode).getId();
             }
            if (child2 == null) {
                Node endNode = createEndNode();
                child2 = dao.addNewNode(endNode).getId();
            }
            nexts.add(child1);
            nexts.add(child2);
        } else {
            // Otherwise, it'll have only one next node.
            Long child = null;
            if (idx != nodeJsonModelList.length - 1) {
                child = dfs(nodeJsonModelList, idx + 1);
            } else {
                Node endNode = createEndNode();
                child = dao.addNewNode(endNode).getId();
            }
            nexts.add(child);
        }
        newNode.setNexts(nexts);
        dao.addNewNode(newNode);
        newNode = dao.searchNodeById(nodeId);
        System.out.println("Name: " + newNode.getName() + "\nID: " + newNode.getId() + " \nChild:" + newNode.getNexts());
        return nodeId;
    }

    ArrayList<Long> nodeIdList = new ArrayList<>();
    @PostMapping("/test/deserializeJourney")
    public void deserializeJourney(@RequestBody String journeyJson) {
        SeDeFunction sede = new SeDeFunction();
        // Map JourneyJson to JourneyJsonModel
        JourneyJsonModel journeyJsonModel = sede.deserializeJounrey(journeyJson);
        // Create Journey object using JourneyJson's info then store in DB
        Journey oneJourney = new Journey();
        oneJourney.setJourneySerialized(journeyJson);
        oneJourney.setJourneyName(journeyJsonModel.getProperties().getJourneyName());
        Long journeyid = dao.addNewJourney(oneJourney).getId();

        // Traverse the journeyJsonModel object and add each node into DB
        dfs(journeyJsonModel.getSequence(), 0);

        // set first node as head
        Node headNode = dao.searchNodeById(nodeIdList.get(0));
        headNode.setHeadOrTail(1); // 1: root, 0: node, -1: leaf
        dao.addNewNode(headNode);
    }

    @PostMapping("/test/deNode")
    public void deNode() {
        Node node2 = dao.searchNodeById(2L);
        Node node3 = dao.searchNodeById(3L);
        Node node4 = dao.searchNodeById(4L);
        Node node5 = dao.searchNodeById(5L);
        Node node6 = dao.searchNodeById(6L);
        Node node7 = dao.searchNodeById(7L);
        Node node8 = dao.searchNodeById(8L);
        Node node9 = dao.searchNodeById(9L);
        Node node10 = dao.searchNodeById(10L);
        Node node11 = dao.searchNodeById(11L);
        Node node12 = dao.searchNodeById(12L);
        Node node13 = dao.searchNodeById(13L);
        Node node14 = dao.searchNodeById(14L);
        Node node15 = dao.searchNodeById(15L);
        Node node16 = dao.searchNodeById(16L);
        System.out.println(node2.getNexts());
        System.out.println(node3.getNexts());
        System.out.println(node4.getNexts());
        System.out.println(node5.getNexts());
        System.out.println(node6.getNexts());
        System.out.println(node7.getNexts());
        System.out.println(node8.getNexts());
        System.out.println(node9.getNexts());
        System.out.println(node10.getNexts());
        System.out.println(node11.getNexts());
        System.out.println(node12.getNexts());
        System.out.println(node13.getNexts());
        System.out.println(node14.getNexts());
        System.out.println(node15.getNexts());
        System.out.println(node16.getNexts());
    }



    @PostMapping("/test/addUser")
    public User addUser(User user){
        return userRepository.save(user);
    }
    //TODO: your own api
}