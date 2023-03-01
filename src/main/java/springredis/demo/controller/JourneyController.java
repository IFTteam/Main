package springredis.demo.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveJourney;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.repository.AudienceListRepository;
import springredis.demo.repository.JourneyRepository;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.activeRepository.ActiveJourneyRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;
import springredis.demo.serializer.SeDeFunction;
import springredis.demo.tasks.CMTExecutor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class JourneyController {
    @Autowired
    private JourneyRepository journeyRepository;
    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ActiveJourneyRepository activeJourneyRepository;

    @Autowired
    private ActiveNodeRepository activeNodeRepository;

    @Autowired
    private AudienceListRepository audienceListRepository;

    @Autowired
    CMTExecutor cmtExecutor;
    @PostMapping("/journey/saveJourney")//保存Journey,仅仅保存Serialized部分
    public Journey saveJourney(@RequestBody String journeyJson){
        nodeIdList.clear();
        System.out.println(journeyJson);
        SeDeFunction sede = new SeDeFunction();

        // Map JourneyJson to JourneyJsonModel
        JourneyJsonModel journeyJsonModel = sede.deserializeJounrey(journeyJson);
        // Create Journey object using JourneyJson's info then store in DB
        String journeyName = journeyJsonModel.getProperties().getJourneyName();
        String frontEndId = journeyJsonModel.getProperties().getJourneyId();
        String thumbNailURL = journeyJsonModel.getProperties().getThumbNailURL();
        int status = journeyJsonModel.getProperties().getStatus();
        String stage = journeyJsonModel.getProperties().getStage();
        String createdBy = journeyJsonModel.getProperties().getCreatedBy();
        String updatedBy = journeyJsonModel.getProperties().getUpdatedBy();
        LocalDateTime createdAt = LocalDateTime.parse(journeyJsonModel.getProperties().getCreatedAt(), DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime updatedAt = LocalDateTime.parse(journeyJsonModel.getProperties().getUpdatedAt(), DateTimeFormatter.ISO_DATE_TIME);
        // If this journey already in DB, we want to modify the existing one instead of storing a new journey.
        Journey existingJourney = journeyRepository.searchJourneyByFrontEndId(frontEndId);

        // Search and store all nodes with JourneyFrontEndId.

        Journey oneJourney = new Journey(journeyName, thumbNailURL, journeyJson, status, stage, frontEndId, createdAt, createdBy, updatedAt, updatedBy);

        if (existingJourney != null) {
            oneJourney.setCreatedAt(existingJourney.getCreatedAt());
            oneJourney.setId(existingJourney.getId());
        }

        return journeyRepository.save(oneJourney);
    }

    @GetMapping("/journey/get-saved-journey/{journeyFrontEndId}")//激活Journey,查取数据库，反序列化
    public String getSavedJourney(@PathVariable("journeyFrontEndId") String journeyFrontEndId){
        String journeyJson = journeyRepository.searchJourneyByFrontEndId(journeyFrontEndId).getJourneySerialized();
        System.out.println(journeyJson);
        return journeyJson;
    }
    @PostMapping("/journey/activateJourney")//激活Journey,查取数据库，反序列化
    public Journey activateJourney(@RequestBody String journeyJson){
        nodeIdList.clear();
        Journey oneJourney = saveJourney(journeyJson);
        SeDeFunction sede = new SeDeFunction();
//         Map JourneyJson to JourneyJsonModel
        JourneyJsonModel journeyJsonModel = sede.deserializeJounrey(journeyJson);
        Long journeyId = journeyRepository.save(oneJourney).getId();
        String journeyFrontEndId = journeyRepository.searchJourneyById(journeyId).getFrontEndId();

//        --------------------------------------------------------------------------------------------------
//        a set to store the result of "findNodesByJourneyFrontEndId".
//        Call DFS to modify/add nodes from the given journeyJson. After each (modify/add) operation, remove the nodeId from the set.
//        The left ones are deleted nodes.
        Node[] queryResult = nodeRepository.searchNodesByJourneyFrontEndId(journeyFrontEndId);
        Set<Long> existingNode = new LinkedHashSet<Long>();

        for (int i = 0; i < queryResult.length; i++) {
            existingNode.add(queryResult[i].getId());
        }
        // Traverse the journeyJsonModel object and add each node into DB
        dfs(journeyJsonModel.getSequence(), 0, journeyFrontEndId);
        for (int i = 0; i < nodeIdList.size(); i++) {
            if (existingNode.contains(nodeIdList.get(i))) {
                existingNode.remove(nodeIdList.get(i));
            }
        }

        nodeRepository.deleteAllById(existingNode);
        for (Long nodeId: existingNode) {
            activeNodeRepository.deleteByNodeId(nodeId);
        }
//        --------------------------------------------------------------------------------------------------

        // Create an ActiveJourney object and map to journey
        ActiveJourney existingActiveJourney = activeJourneyRepository.searchActiveJourneyByJourneyId(journeyId);

        // update activeNodes
        if (existingActiveJourney == null) {
            ActiveJourney activeJourney = new ActiveJourney();
            activeJourney.setJourneyId(journeyId);
            activeJourneyRepository.save(activeJourney);
            createActiveNodesAndMapToNodes(activeJourney);

        } else {
            createActiveNodesAndMapToNodes(existingActiveJourney);
        }

        // set first node as head
        Node headNode = nodeRepository.searchNodeByid(nodeIdList.get(0));
        headNode.setHeadOrTail(1); // 1: root, 0: node, -1: leaf

        // Set dummyHead
        Node dummyHead = nodeRepository.searchNodeByFrontEndId("dummyHead" + journeyFrontEndId);
        if (dummyHead == null) {
            dummyHead = new Node();
            dummyHead.setFrontEndId("dummyHead" + journeyFrontEndId);
        }
        List<Long> nexts = new ArrayList<>();
        nexts.add(headNode.getId());
        dummyHead.setNexts(nexts);
        Long dummyHeadId = nodeRepository.save(dummyHead).getId();
        ActiveJourney activeJourney = activeJourneyRepository.searchActiveJourneyByJourneyId(journeyId);
        // Set activeDummyHead who corresponds to dummyHead
        ActiveNode activeDummyHead = activeNodeRepository.findByDBNodeId(dummyHeadId);
        if (activeDummyHead == null) {
            activeDummyHead = new ActiveNode();
            activeDummyHead.setNodeId(dummyHeadId);
            activeDummyHead.setActiveJourney(activeJourney);
        }

        activeNodeRepository.save(activeDummyHead);
        nodeRepository.save(headNode);
        nodeRepository.save(dummyHead);

        //get audience list from properties
        String audienceListName = GetAudienceListName(headNode.getId());
        List<Long> audienceList = AudienceFromAudienceList(audienceListName);
        System.out.println(audienceList);

        // Call CoreModuleTask
        CoreModuleTask cmt = new CoreModuleTask();
        cmt.setNodeId(dummyHeadId);
        cmt.setAudienceId1(audienceList);
        cmt.setCallapi(0);
        cmt.setTaskType(1);
        cmtExecutor.execute(cmt);

        return oneJourney;
    }


    private List<Long> AudienceFromAudienceList(String audienceListName){
        AudienceList audienceList = audienceListRepository.searchAudienceListByName(audienceListName);
        List<Audience> audiences = audienceList.getAudiences();
        List<Long> audiencesId= new ArrayList<>();
        for(Audience audience: audiences){
            audiencesId.add(audience.getId());
        }
        return audiencesId;
    }


    private String GetAudienceListName(Long nodeId){
        Node currentNode = nodeRepository.findById(nodeId).get();
        String properties = currentNode.getProperties();
        JSONObject jsonObject = new JSONObject(properties);
        String name = jsonObject.getString("list");
        return name;
    }

    //TODO: Node和Journey级联关系没保存，要写一下
    private Journey JourneyParse(Journey journey) {
        //Deserialize function
        SeDeFunction seDeFunction = new SeDeFunction();
        List<Node> deserializedJourney =  seDeFunction.deserializing(journey.getJourneySerialized());

        //Setup active journey
        ActiveJourney activeJourney = new ActiveJourney();
        activeJourney.setJourneyId(journey.getId());
        activeJourneyRepository.save(activeJourney);
        //Initialize Journey function
        int n = deserializedJourney.size();
        System.out.println(n);
        System.out.println(deserializedJourney.get(0).getNexts());
        //1.Use map frontEndId->BackEndId and replace the node nexts frontEndId->BackEndId
        HashMap<String,Long> keyHash = new HashMap<>();
        List<Node> heads = new ArrayList<>();
        for (int i=0; i<n; i++){
            deserializedJourney.get(i).nextsSerialize();
            deserializedJourney.set(i,nodeRepository.save(deserializedJourney.get(i)));
            deserializedJourney.get(i).nextsDeserialize();
            keyHash.put(deserializedJourney.get(i).getFrontEndId(),deserializedJourney.get(i).getId());
            //set up active node
            ActiveNode activeNode = new ActiveNode();
            activeNode.setActiveJourney(activeJourney);
            activeNode.setNodeId(deserializedJourney.get(i).getId());
            activeNodeRepository.save(activeNode);
        }
        System.out.println(keyHash);
        // replace nexts ID

        for (int i=0; i<n; i++){
            Node nodeI = deserializedJourney.get(i);
            List<Long> nexts = nodeI.getNexts();
            System.out.println(nexts);
            for (int j=0; j<nexts.size(); j++){
                nexts.set(j, keyHash.get(nexts.get(j)));
            }
            nodeI.setNexts(nexts);
            System.out.println(nexts);
            nodeI.nextsSerialize();
            nodeRepository.save(deserializedJourney.get(i));
            if (nodeI.getHeadOrTail()==1){//add to start list if the node is a start node
                heads.add(nodeI);
            }
        }
        //2.Start Journey from start node
        for(int i=0; i<heads.size();i++){
            CoreModuleTask coreModuleTask = new CoreModuleTask();
            coreModuleTask.setNodeId(heads.get(i).getId());
            //Dummy Task
            coreModuleTask.setTargetNodeId(activeNodeRepository.findByDBNodeId(heads.get(i).getId()).getId());//Target node ->source
            cmtExecutor.execute(coreModuleTask);
//            taskExecutor.callModule(heads.get(i));
        }
        return journey;
    }


    private Node createEndNode(String journeyFrontEndId) {
        Node endNode = new Node();
        endNode.setType("end");
        endNode.setHeadOrTail(2);
        endNode.setName("endNode");
        endNode.setJourneyFrontEndId(journeyFrontEndId);
        return endNode;
    }
    private Node createNodeFromNodeJsonModel(NodeJsonModel nodeJsonModel, String journeyFrontEndId) {
        LocalDateTime createdAt = LocalDateTime.parse(nodeJsonModel.getCreatedAt(), DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime updatedAt = LocalDateTime.parse(nodeJsonModel.getUpdatedAt(), DateTimeFormatter.ISO_DATE_TIME);
        String name = nodeJsonModel.getName();
        String type  = nodeJsonModel.getComponentType();
        String status = nodeJsonModel.getStatus();
        String createdBy = nodeJsonModel.getCreatedBy();
        String updatedBy = nodeJsonModel.getUpdatedBy();
        String frontEndId = nodeJsonModel.getId();
        NodeJsonModel.Property properties = nodeJsonModel.getProperties();
//        System.out.println("PPPPPPPPPPPPPP" + properties);
        String propertiesString = new SeDeFunction().serializeNodeProperty(properties);
//        System.out.println("BBBBBBBBBBBBBB" + propertiesString);
        Node newNode = new Node(name, type, status, createdAt, createdBy, updatedAt, updatedBy, journeyFrontEndId, propertiesString);
        newNode.setHeadOrTail(0);
        newNode.setFrontEndId(frontEndId);

        // If the user modifies the journey, we only want to keep the original version nodes' creation date.
        Node existingNode = nodeRepository.searchNodeByFrontEndId(newNode.getFrontEndId());
        if (existingNode != null) {
            newNode.setCreatedAt(existingNode.getCreatedAt());
            newNode.setId(existingNode.getId());
        }
        return newNode;
    }
    public Long dfs(NodeJsonModel[] nodeJsonModelList, int idx, String journeyFrontEndId) {
        Node newNode = createNodeFromNodeJsonModel(nodeJsonModelList[idx], journeyFrontEndId);
        // We need to store the node in DB first
        nodeRepository.save(newNode);
        // so that we can get the node's id
        Long nodeId = newNode.getId();
        nodeIdList.add(newNode.getId());
        newNode = nodeRepository.searchNodeByid(nodeId);

        List<Long> nexts = new ArrayList<>();
        // If it is an if/else node. It'll have two next nodes.
        if (newNode.getType().equals("switch")) {
            Long child1 = null;
            Long child2 = null;
            if (nodeJsonModelList[idx].getBranches().getTrue().length != 0) {
                child1 = dfs(nodeJsonModelList[idx].getBranches().getTrue(), 0, journeyFrontEndId);
            }
            if (nodeJsonModelList[idx].getBranches().getFalse().length != 0) {
                child2 = dfs(nodeJsonModelList[idx].getBranches().getFalse(), 0, journeyFrontEndId);
            }
            if (child1 == null) {
                Node endNode = createEndNode(journeyFrontEndId);
                child1 = nodeRepository.save(endNode).getId();
                nodeIdList.add(endNode.getId());
            }
            if (child2 == null) {
                Node endNode = createEndNode(journeyFrontEndId);
                child2 = nodeRepository.save(endNode).getId();
                nodeIdList.add(endNode.getId());
            }
            nexts.add(child1);
            nexts.add(child2);
        } else {
            // Otherwise, it'll have only one next node.
            Long child = null;
            if (idx != nodeJsonModelList.length - 1) {
                child = dfs(nodeJsonModelList, idx + 1, journeyFrontEndId);
            } else {
                Node endNode = createEndNode(journeyFrontEndId);
                child = nodeRepository.save(endNode).getId();
                nodeIdList.add(endNode.getId());
            }
            nexts.add(child);
        }
        newNode.setNexts(nexts);
        nodeRepository.save(newNode);
        newNode = nodeRepository.searchNodeByid(nodeId);
        System.out.println("Name: " + newNode.getName() + "\nID: " + newNode.getId() + " \nChild:" + newNode.getNexts() + " \nJourneyFrontEndId:"+journeyFrontEndId);
        return nodeId;
    }

    ArrayList<Long> nodeIdList = new ArrayList<>();
    private void createActiveNodesAndMapToNodes(ActiveJourney activeJourney) {
        for (int i = 0; i < nodeIdList.size(); i++) {
            System.out.println(nodeIdList.get(i));
            ActiveNode existingActiveNode = activeNodeRepository.findByDBNodeId(nodeIdList.get(i));
            if (existingActiveNode == null) {
                ActiveNode activeNode = new ActiveNode();
                activeNode.setActiveJourney(activeJourney);
                activeNode.setNodeId(nodeIdList.get(i));
                activeNodeRepository.save(activeNode);
            }
        }
    }

}
