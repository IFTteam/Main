package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import springredis.demo.Service.DAO;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.repository.*;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;
import org.springframework.http.HttpStatus;
import springredis.demo.serializer.SeDeFunction;
import springredis.demo.tasks.CMTExecutor;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    CMTExecutor cmtExecutor;

    private String str = new String(),str2=new String();

    private List<CoreModuleTask> tasks = new ArrayList<>();


    //audience transfer test
    @GetMapping("/active_audience_transfer_test")
    @ResponseBody
    public List<CoreModuleTask> simulated_core_module_call(){
        //create a node and a corresponding active node, as well as two nodes that are its nexts.
        // Create four audience entities, two of which has their corresponding active audience already mapped to this node, and the other two are unregistered in active DB. (simulating webhook trigger hit)
        //then, make two CMTs with node being this node (and its corresponding active node). First fill the two audiencelists with two new active audiences, then call the "createUser" api in task controller.
        // This should create the two new active audiences1 in the active audience pool of the respective two next nodes.
        //second CMT will fill the two audiencelists with the two existing active audiences(respectively), then call "moveUser" in task controller.
        // This will result in these two audience's mapped-to active node become this node's next two nodes, respectively.
        Node node1=new Node(),node2=new Node(),node3=new Node();
        Long nid2 = dao.addNewNode(node2).getId(), nid3= dao.addNewNode(node3).getId();;
        List<Long> tmp = new ArrayList<>(); tmp.add(nid2); tmp.add(nid3);
        node1.setNexts(tmp);
        Long nid1 = dao.addNewNode(node1).getId();
        System.out.println(node1.getNexts().get(0).toString());
        ActiveNode act1= new ActiveNode(),act2=new ActiveNode(),act3 = new ActiveNode();
        act1.setNodeId(nid1);  act2.setNodeId(nid2); act3.setNodeId(nid3);
        act1 = dao.addNewActiveNode(act1);act2 = dao.addNewActiveNode(act2); act3 = dao.addNewActiveNode(act3);
        Long aud1id=dao.addNewAudience(new Audience()).getId(),aud2id=dao.addNewAudience(new Audience()).getId(),aud3id=dao.addNewAudience(new Audience()).getId(),aud4id=dao.addNewAudience(new Audience()).getId();
        ActiveAudience actaud1 = new ActiveAudience(), actaud2 = new ActiveAudience();
        actaud1.setAudienceId(aud1id);actaud2.setAudienceId(aud2id);                //only first two audience are registered!
        actaud1.setActiveNode(act1); actaud2.setActiveNode(act1);;
        actaud1 = dao.addNewActiveAudience(actaud1);actaud2 = dao.addNewActiveAudience(actaud2);
        //configure cmt1
        CoreModuleTask cmt1 = new CoreModuleTask(), cmt2 = new CoreModuleTask();
        List<Long> audlist = new ArrayList<>(); audlist.add(aud1id);cmt1.setAudienceId1(audlist);
        List<Long> audlist2 = new ArrayList<>(); audlist2.add(aud2id);cmt1.setAudienceId2(audlist2);cmt1.setNodeId(nid1);cmt1.setName("hi");
        //configure cmt2
        List<Long> audlist3 = new ArrayList<>(); audlist3.add(aud3id);cmt2.setAudienceId1(audlist3);
        List<Long> audlist4 = new ArrayList<>(); audlist4.add(aud4id);cmt2.setAudienceId2(audlist4);cmt2.setNodeId(nid1);cmt1.setName("hi");
        String url1 = "http://localhost:8080/move_user";
        restTemplate.exchange(url1,HttpMethod.POST,new HttpEntity<>(cmt1),Long.class).getBody();
        url1 = "http://localhost:8080/create_user";
        restTemplate.exchange(url1,HttpMethod.POST,new HttpEntity<>(cmt2),Long.class);
        List<CoreModuleTask> res = new ArrayList<>();
        res.add(cmt1);res.add(cmt2);
        return res;
    }


    //task executor test
    // make three nodes - the first node is an api_trigger_node with name being a test endpoint that returns the task directly without doing anything;
    // the second node is its successive node with a "time delay" type node with random name. The third node is a node with type being "End".
    // Each node has a associated (active) audience, labeled aud1,aud2,aud3.
    // test1: We will initialize task executor with a CMT made from the first node and makenext being 1,and tasktype being 0.
    // This means the task executor should first call corresponding method in APi_trigger controller,
    // then upon return, transfer active audience from first node to next node (node with type being "time-delay"),
    // and finally push a CMT onto global task queue with task being about the next node (time-delay).
    // test 1.5: We will initialize task executor with a CMT made from the first node and makenext being 1, and tasktype being 1.
    // This means the task executor should first call corresponding method in APi_trigger controller,
    // then upon return, create new active audience in  next active node (node with type being "time-delay"),
    // and finally push a CMT onto global task queue with task being about the next node (time-delay).
    // test2: We will initialize task executor with a CMT made from the first node and makenext being 0.
    // sThis means the task executor should only call the target endpoint, but upon return, do not do anymore audience-transfer or task-making activity and simply return.
    // test3: We will  initialize task executor with a CMT made from the "End" node. This should do nothing and simply return.
    @GetMapping(value="/task_executor_test")
    @ResponseBody
    public CoreModuleTask task_executor_test(){
        //preparation
        Node node1 = new Node(), node2 = new Node(); node1.setType("APITrigger"); node1.setName("Test"); node2.setType("Time_Delay"); node2.setName("Test");
        Long node2id = nodeRepository.save(node2).getId();
        List<Long> tmp = new ArrayList<>(); tmp.add(node2id);
        node1.setNexts(tmp);
        Long node1id = nodeRepository.save(node1).getId();
        ActiveNode acn = new ActiveNode(); acn.setNodeId(node1id);
        Long acnid1 = activeNodeRepository.save(acn).getId();
        ActiveNode acn2 = new ActiveNode(); acn2.setNodeId(node2id);
        Long acnid2 = activeNodeRepository.save(acn2).getId();
        Long aud1id=dao.addNewAudience(new Audience()).getId(),aud2id=dao.addNewAudience(new Audience()).getId();
        ActiveAudience actaud1 = new ActiveAudience(), actaud2 = new ActiveAudience();
        actaud1.setAudienceId(aud1id);actaud2.setAudienceId(aud2id);                //only first two audience are registered!
        actaud1.setActiveNode(acn); actaud2.setActiveNode(acn);;
        Long actaud1id = dao.addNewActiveAudience(actaud1).getId(),actaud2id = dao.addNewActiveAudience(actaud2).getId();
       //making core module task:
        CoreModuleTask cmt1 = new CoreModuleTask();
        cmt1.setNodeId(node1id);
        List<Long> list = new ArrayList<>();
        list.add(aud1id); list.add(aud2id);
        List<Long> list2 = new ArrayList<>();
        list2.add(actaud1id);list2.add(actaud2id);
        cmt1.setAudienceId1(list);
        cmt1.setActiveAudienceId1(list2);
        cmt1.setTaskType(0);
        cmt1.setMakenext(1);
        cmt1.setName("Test");cmt1.setType("APITrigger");
        //execute this task with task executor
        cmtExecutor.execute(cmt1);
        return cmt1;
    }

    @GetMapping(value="/removeall")
    public void removall() {
//        List<ActiveNode> ls2 = activeNodeRepository.findAll();
//        for (ActiveNode n : ls2) {
//            activeNodeRepository.delete(n);
//        }
//        List<ActiveAudience> ls1 = activeAudienceRepository.findAll();
//        for (ActiveAudience n : ls1) {
//            activeAudienceRepository.delete(n);
//        }
//        List<Audience> ls3 = audienceRepository.findAll();
//        for (Audience n : ls3) {
//            audienceRepository.delete(n);
//        }
        List<Node> ls4 = nodeRepository.findAll();
        for (Node n : ls4) {
            nodeRepository.delete(n);
        }
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
        Journey j = journeyRepository.searchJourneyById(1L);
        System.out.println(j);
//        SeDeFunction sede = new SeDeFunction();
//        // Map JourneyJson to JourneyJsonModel
//        JourneyJsonModel journeyJsonModel = sede.deserializeJounrey(journeyJson);
//        // Create Journey object using JourneyJson's info then store in DB
//        Journey oneJourney = new Journey();
//        oneJourney.setJourneySerialized(journeyJson);
//        oneJourney.setJourneyName(journeyJsonModel.getProperties().getJourneyName());
//        Long journeyid = dao.addNewJourney(oneJourney).getId();
//
//        // Traverse the journeyJsonModel object and add each node into DB
//        dfs(journeyJsonModel.getSequence(), 0);
//
//        // set first node as head
//        Node headNode = dao.searchNodeById(nodeIdList.get(0));
//        headNode.setHeadOrTail(1); // 1: root, 0: node, -1: leaf
//        dao.addNewNode(headNode);
    }

    @PostMapping("/test/deNode")
    public void deNode() {
        Node node1 = dao.searchNodeById(13L);
        Node node2 = dao.searchNodeById(3L);
        Node node3 = dao.searchNodeById(19L);
        Node node4 = dao.searchNodeById(20L);
//        Node node10 = dao.searchNodeById(10L);
//        Node node11 = dao.searchNodeById(11L);
//        Node node12 = dao.searchNodeById(12L);
//        Node node13 = dao.searchNodeById(13L);
//        Node node14 = dao.searchNodeById(14L);
//        Node node15 = dao.searchNodeById(15L);
//        Node node16 = dao.searchNodeById(16L);
        System.out.println(node1.getNexts());
        System.out.println(node2.getNexts());
        System.out.println(node3.getNexts());
        System.out.println(node4.getNexts());
//        System.out.println(node10.getNexts());
//        System.out.println(node11.getNexts());
//        System.out.println(node12.getNexts());
//        System.out.println(node13.getNexts());
//        System.out.println(node14.getNexts());
//        System.out.println(node15.getNexts());
//        System.out.println(node16.getNexts());
    }



    @PostMapping("/test/addUser")
    public User addUser(User user){
        return userRepository.save(user);
    }
    //TODO: your own api
    
    @GetMapping("/test/addDelayTask")
    public TimeTask addTask() {  //for testing purpose by Delong Bao
    	CoreModuleTask coreModuleTask = new CoreModuleTask(new BaseTaskEntity());
    	coreModuleTask.setId((long) 1234567);
    	Node node = new Node();
    	node.setName("DelayTimeInSecond 5 1 1");
    	node.getNexts().add((long) 1);
    	node.getNexts().add((long) 4);
    	node.nextsSerialize();
    	System.out.println("Node sNexts===================================" + node.getSNexts());
    	nodeRepository.save(node);  //node id is only generated when you save it into repository
    	System.out.println(nodeRepository.findAll());
    	System.out.println("==============================TestController Node ID: " + node.getId());
    	coreModuleTask.setNodeId(node.getId());
    	System.out.println("==============================TestController CoreModuleTask Node ID: " + coreModuleTask.getNodeId());
    	TimeTask timeTask = restTemplate.postForObject("http://localhost:8080/addNewTask", coreModuleTask, TimeTask.class, TimeTask.class);
    	return timeTask;
    }
    
    @GetMapping("/test/addSpecificTask")
    public TimeTask addTask1() {  //for testing purpose by Delong Bao
    	CoreModuleTask coreModuleTask = new CoreModuleTask(new BaseTaskEntity());
    	coreModuleTask.setId((long) 1234567);
    	Node node = new Node();
    	node.setName("SpecificTime 2022-11-06 17:25:10 1 1");
    	node.getNexts().add((long) 1);
    	node.getNexts().add((long) 4);
    	node.nextsSerialize();
    	System.out.println("Node sNexts===================================" + node.getSNexts());
    	nodeRepository.save(node);  //node id is only generated when you save it into repository
    	System.out.println(nodeRepository.findAll());
    	System.out.println("==============================TestController Node ID: " + node.getId());
    	coreModuleTask.setNodeId(node.getId());
    	System.out.println("==============================TestController CoreModuleTask Node ID: " + coreModuleTask.getNodeId());
    	TimeTask timeTask = restTemplate.postForObject("http://localhost:8080/addNewTask", coreModuleTask, TimeTask.class, TimeTask.class);
    	return timeTask;
    }
    
    @GetMapping("/test/addUnitTask")
    public TimeTask addTask2() {  //for testing purpose by Delong Bao
    	CoreModuleTask coreModuleTask = new CoreModuleTask(new BaseTaskEntity());
    	coreModuleTask.setId((long) 1234567);
    	Node node = new Node();
    	node.setName("TimeUnit 1 HOURS 1 1");
    	node.getNexts().add((long) 1);
    	node.getNexts().add((long) 4);
    	node.nextsSerialize();
    	System.out.println("Node sNexts===================================" + node.getSNexts());
    	nodeRepository.save(node);  //node id is only generated when you save it into repository
    	System.out.println(nodeRepository.findAll());
    	System.out.println("==============================TestController Node ID: " + node.getId());
    	coreModuleTask.setNodeId(node.getId());
    	System.out.println("==============================TestController CoreModuleTask Node ID: " + coreModuleTask.getNodeId());
    	TimeTask timeTask = restTemplate.postForObject("http://localhost:8080/addNewTask", coreModuleTask, TimeTask.class, TimeTask.class);
    	return timeTask;
    }
    
    @GetMapping("/test/addUser")
    public User addUser() {
    	User user = new User();
    	userRepository.save(user);
    	return user;
    }
}