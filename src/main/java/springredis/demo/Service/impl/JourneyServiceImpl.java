package springredis.demo.Service.impl;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import springredis.demo.Service.JourneyService;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveJourney;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.error.DataBaseObjectNotFoundException;
import springredis.demo.error.JourneyNotFoundException;
import springredis.demo.repository.AudienceListRepository;
import springredis.demo.repository.JourneyRepository;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.UserRepository;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveJourneyRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;
import springredis.demo.serializer.SeDeFunction;
import springredis.demo.structures.OutAPICaller;
import springredis.demo.tasks.CMTExecutor;
import springredis.demo.utils.OptionalUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class JourneyServiceImpl implements JourneyService {

    private final JourneyRepository journeyRepository;

    private final NodeRepository nodeRepository;

    private final ActiveNodeRepository activeNodeRepository;

    private final ActiveAudienceRepository activeAudienceRepository;

    private final ActiveJourneyRepository activeJourneyRepository;

    private final UserRepository userRepository;

    private final AudienceListRepository audienceListRepository;

    @Autowired
    private CMTExecutor cmtExecutor;

    private ArrayList<Long> nodeIdList = new ArrayList<>();

    @Autowired
    public JourneyServiceImpl(JourneyRepository journeyRepository, NodeRepository nodeRepository,
                              ActiveNodeRepository activeNodeRepository, ActiveAudienceRepository activeAudienceRepository,
                              ActiveJourneyRepository activeJourneyRepository, UserRepository userRepository,
                              AudienceListRepository audienceListRepository) {
        this.journeyRepository = journeyRepository;
        this.nodeRepository = nodeRepository;
        this.activeNodeRepository = activeNodeRepository;
        this.activeAudienceRepository = activeAudienceRepository;
        this.activeJourneyRepository = activeJourneyRepository;
        this.userRepository = userRepository;
        this.audienceListRepository = audienceListRepository;
    }

    @Override
    public Journey save(String journeyJson) {
        SeDeFunction sede = new SeDeFunction();
        // Map JourneyJson to JourneyJsonModel
        JourneyJsonModel journeyJsonModel = sede.deserializeJourney(journeyJson);
        System.out.println("The properties is: " + journeyJsonModel.getProperties());

        // Create Journey object using JourneyJson's info then store in DB
        String journeyName = journeyJsonModel.getProperties().getJourneyName();
        String frontEndId = journeyJsonModel.getProperties().getJourneyId();
        String thumbNailURL = journeyJsonModel.getProperties().getThumbNailURL();
        String stage = journeyJsonModel.getProperties().getStage();
        String createdBy = journeyJsonModel.getProperties().getCreatedBy();
        String updatedBy = journeyJsonModel.getProperties().getUpdatedBy();
        LocalDateTime createdAt = LocalDateTime.parse(journeyJsonModel.getProperties().getCreatedAt(), DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime updatedAt = LocalDateTime.parse(journeyJsonModel.getProperties().getUpdatedAt(), DateTimeFormatter.ISO_DATE_TIME);

        // If this journey already in DB, we want to modify the existing one instead of storing a new journey.
        Journey existingJourney = journeyRepository.searchJourneyByFrontEndId(frontEndId);
        // check existing journey status code
        // if existing journey is not null and its status indicate activating, then stop saving
        if (existingJourney != null && existingJourney.getStatus().equals(Journey.ACTIVATING)) {
            log.info("The journey now is activating, so no need to save during activating.");
            return null;
        }
        nodeIdList.clear();

        // Search and store all nodes with JourneyFrontEndId.
        Journey oneJourney = new Journey(journeyName, thumbNailURL, journeyJson, Journey.NOT_ACTIVATE, stage, frontEndId, createdAt, createdBy, updatedAt, updatedBy);
        if (existingJourney != null) {
            oneJourney.setCreatedAt(existingJourney.getCreatedAt());
            oneJourney.setId(existingJourney.getId());
            // if existing journey is not null, then follow the old status
            oneJourney.setStatus(existingJourney.getStatus());
        }

        return journeyRepository.save(oneJourney);
    }


    public Journey setJourneyStatus(JourneyJsonModel journeyJsonModel, int status) {
        String journeyFrontEndId = journeyJsonModel.getProperties().getJourneyId();
        Journey existingJourney = journeyRepository.searchJourneyByFrontEndId(journeyFrontEndId);
        if (existingJourney == null) {
            throw new JourneyNotFoundException("Journey not found by given journey front-end id!");
        }

        if (status == Journey.ACTIVATED_FINISHED) {
            endJourney(existingJourney.getId());
        }
        else {
            log.info("set journey status from {} to {}", existingJourney.getStatus(), status);
            existingJourney.setStatus(status);
        }
        String updatedBy = journeyJsonModel.getProperties().getUpdatedBy();
        LocalDateTime updatedAt = LocalDateTime.parse(journeyJsonModel.getProperties().getUpdatedAt(), DateTimeFormatter.ISO_DATE_TIME);
        existingJourney.setUpdatedAt(updatedAt);
        existingJourney.setUpdatedBy(updatedBy);
        return journeyRepository.save(existingJourney);
    }

    @Override
    public String getSavedJourneyByJourneyFrontEndId(String journeyFrontEndId) {
        String journeyJson = journeyRepository.searchJourneyByFrontEndId(journeyFrontEndId).getJourneySerialized();
        log.info(journeyJson);
        return journeyJson;
    }

    @Override
    public Journey activate(String journeyJson) throws JourneyNotFoundException {
        nodeIdList.clear();
        Journey oneJourney = save(journeyJson);
        SeDeFunction sede = new SeDeFunction(); // class SeDeFunction : 传进来的是node list，把一个一个node拿出来序列化，然后加入String，返回String
//         Map JourneyJson to JourneyJsonModel
        JourneyJsonModel journeyJsonModel = sede.deserializeJourney(journeyJson);
        // set journey status to ACTIVATING(1), meaning the journey is activating
        setJourneyStatus(journeyJsonModel, Journey.ACTIVATING);
        Long journeyId = journeyRepository.save(oneJourney).getId();
        String journeyFrontEndId = journeyRepository.searchJourneyById(journeyId).getFrontEndId();

        /*
         *   a set to store the result of "findNodesByJourneyFrontEndId".
         *   Call DFS to modify/add nodes from the given journeyJson. After each (modify/add) operation, remove the nodeId from the set.
         *   The left ones are deleted nodes.
         */
        Node[] queryResult = nodeRepository.searchNodesByJourneyFrontEndId(journeyFrontEndId);
        Set<Long> existingNode = new LinkedHashSet<Long>();

        for (int i = 0; i < queryResult.length; i++) {
            existingNode.add(queryResult[i].getId());
        }
        // Traverse the journeyJsonModel object and add each node into DB
        System.out.println("The node list before dfs is: " + nodeIdList);
        System.out.println("========================== DFS started ==========================");
        if (journeyJsonModel.getSequence().length <= 0) {
            return oneJourney;
        }
        dfs(journeyJsonModel.getSequence(), 0, journeyFrontEndId);
        System.out.println("=========================== DFS ended ===========================");
        System.out.println("The node list after dfs is: " + nodeIdList);
        for (int i = 0; i < nodeIdList.size(); i++) {
            if (existingNode.contains(nodeIdList.get(i))) {
                existingNode.remove(nodeIdList.get(i));
            }
        }
        System.out.println("The node List2 is " + nodeIdList);

        nodeRepository.deleteAllById(existingNode);
        for (Long nodeId : existingNode) {
            System.out.println("active node id: " + activeNodeRepository.findByDBNodeId(nodeId).getId());
            System.out.println("Deleting node: " + nodeId);
            activeAudienceRepository.deleteByActiveNodeId(activeNodeRepository.findByDBNodeId(nodeId).getId());
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
        System.out.println("The node List is " + nodeIdList);
        Node headNode = nodeRepository.searchNodeByid(nodeIdList.get(0));
        if (headNode == null) {
            System.out.println("The headNode is null");
        } else {
            System.out.println("The headNode is" + headNode);
        }
        headNode.setHeadOrTail(1); // 1: root, 0: node, 2: leaf

        // Dummy head initialization
        Node dummyHead = nodeRepository.searchNodeByFrontEndId("dummyHead" + journeyFrontEndId);
        if (dummyHead == null) {
            dummyHead = new Node();
            dummyHead.setFrontEndId("dummyHead" + journeyFrontEndId);
        }
        List<Long> nexts = new ArrayList<>();
        nexts.add(headNode.getId());
        dummyHead.setNexts(nexts);
        dummyHead.nextsSerialize();
        dummyHead.setCreatedAt(LocalDateTime.now());
        dummyHead.setUpdatedAt(LocalDateTime.now());
        dummyHead.setCreatedBy("System");
        dummyHead.setUpdatedBy("System");
        dummyHead.setName("dummyHead");
        dummyHead.setEndNodesCount(headNode.getEndNodesCount());
        dummyHead.setJourneyFrontEndId(journeyFrontEndId);

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


        // Call CoreModuleTask
        CoreModuleTask cmt = new CoreModuleTask();
        cmt.setNodeId(dummyHeadId);
        cmt.setCallapi(0);
        cmt.setTaskType(1);
        cmt.setJourneyId(journeyId);
        cmt.setName("dummyHead");
        cmt.setType("dummy");
        System.out.println("Journey Id is " + journeyId);

        //get audience list from properties
        long userId = Long.parseLong(oneJourney.getCreatedBy());
        System.out.println("UserId is " + userId);
        List<Long> audienceList = AudienceFromAudienceList(headNode.getId(), userId);
        cmt.setAudienceId1(audienceList);

        System.out.println("Audience List 1 is:" + cmt.getAudienceId1().toString());
        System.out.println("======================= Moving to CMTExecutor ========================");

        // after being activated, but before execute, the journey status should label as ACTIVATED_RUNNING(3),
        // meaning the journey already activated and is running
        setJourneyStatus(journeyJsonModel, Journey.ACTIVATED_RUNNING);

        cmtExecutor.execute(cmt);
        return oneJourney;
    }

    @Transactional
    private Long dfs(NodeJsonModel[] nodeJsonModelList, int idx, String journeyFrontEndId) {
        Node newNode = createNodeFromNodeJsonModel(nodeJsonModelList[idx], journeyFrontEndId);
        System.out.println(nodeJsonModelList[idx].toString());
        // We need to store the node in DB first
        //nodeRepository.save(newNode);
        // so that we can get the node's id
        nodeRepository.save(newNode);
        System.out.println("The new node is: " + newNode);

        Long nodeId = newNode.getId();
        nodeIdList.add(nodeId);
        System.out.println("The node List in [" + idx + "] is " + nodeIdList);
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
        newNode.nextsSerialize();
        Integer endNodesCount = 0;
        for (Long next : nexts) {
            Optional<Node> optionalNode = nodeRepository.findById(next);
            if (optionalNode.isEmpty()) {
                throw new DataBaseObjectNotFoundException("not found");
            }
            Node node = optionalNode.get();
            endNodesCount += node.getEndNodesCount();
        }
        newNode.setEndNodesCount(endNodesCount);
        nodeRepository.save(newNode);
        System.out.println("Name: " + newNode.getName() + "\nID: " + newNode.getId() + " \nChild:" + newNode.getNexts() + " \nJourneyFrontEndId:" + journeyFrontEndId);
        return nodeId;
    }


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

    private Node createNodeFromNodeJsonModel(NodeJsonModel nodeJsonModel, String journeyFrontEndId) {
        LocalDateTime createdAt = LocalDateTime.parse(nodeJsonModel.getCreatedAt(), DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime updatedAt = LocalDateTime.parse(nodeJsonModel.getUpdatedAt(), DateTimeFormatter.ISO_DATE_TIME);
        String name = nodeJsonModel.getName();
        String type = nodeJsonModel.getComponentType();
        String status = nodeJsonModel.getStatus();
        String createdBy = nodeJsonModel.getCreatedBy();
        String updatedBy = nodeJsonModel.getUpdatedBy();
        String frontEndId = nodeJsonModel.getId();
        NodeJsonModel.Property properties = nodeJsonModel.getProperties();
        String propertiesString = new SeDeFunction().serializeNodeProperty(properties);
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

    private List<Long> AudienceFromAudienceList(Long nodeId, long userId) {
        System.out.println("current node ID is:" + nodeId.toString());
        Optional<Node> optionalNode = nodeRepository.findById(nodeId);
        Node currentNode = OptionalUtils.getObjectOrThrow(optionalNode, "Not found current Node by given node id");
        String properties = currentNode.getProperties();
        JSONObject jsonObject = new JSONObject(properties);
        System.out.println("object is:" + jsonObject);

        String name = jsonObject.getString("list");
        // if any list, return all list of that user
        List<Long> audiencesId = new ArrayList<>();
        List<Audience> audiences = new ArrayList<>();
        if (name.equals("Any list")) {
            User user = userRepository.findById(userId);
            List<AudienceList> listList = audienceListRepository.findByUser(user);
            for (AudienceList audiencelist : listList) {
                audiences.addAll(audiencelist.getAudiences());
            }
        } else {
            // user input specific list name. e.g. List A
            AudienceList audienceList = audienceListRepository.searchAudienceListByName(name);
            audiences = audienceList.getAudiences();
        }
        for (Audience audience : audiences) {
            audiencesId.add(audience.getId());
        }
        return audiencesId;
    }

    private Node createEndNode(String journeyFrontEndId) {
        Node endNode = new Node();
        endNode.setType("end");
        endNode.setHeadOrTail(2);
        endNode.setName("endNode");
        endNode.setJourneyFrontEndId(journeyFrontEndId);
        endNode.setEndNodesCount(1);
        return endNode;
    }

    // TODO: Node和Journey级联关系没保存，要写一下
    public Journey journeyParse(Journey journey) {
        //Deserialize function
        SeDeFunction seDeFunction = new SeDeFunction();
        List<Node> deserializedJourney = seDeFunction.deserializing(journey.getJourneySerialized());

        //Setup active journey
        ActiveJourney activeJourney = new ActiveJourney();
        activeJourney.setJourneyId(journey.getId());
        activeJourneyRepository.save(activeJourney);
        //Initialize Journey function
        int n = deserializedJourney.size();
        System.out.println("the deserializedJourney size is:" + n);
        System.out.println(deserializedJourney.get(0).getNexts());
        //1.Use map frontEndId->BackEndId and replace the node nexts frontEndId->BackEndId
        HashMap<String, Long> keyHash = new HashMap<>();
        List<Node> heads = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            deserializedJourney.get(i).nextsSerialize();
            deserializedJourney.set(i, nodeRepository.save(deserializedJourney.get(i)));
            deserializedJourney.get(i).nextsDeserialize();
            keyHash.put(deserializedJourney.get(i).getFrontEndId(), deserializedJourney.get(i).getId());
            //set up active node
            ActiveNode activeNode = new ActiveNode();
            activeNode.setActiveJourney(activeJourney);
            activeNode.setNodeId(deserializedJourney.get(i).getId());
            activeNodeRepository.save(activeNode);
        }
        System.out.println(keyHash);
        // replace nexts ID

        for (int i = 0; i < n; i++) {
            Node nodeI = deserializedJourney.get(i);
            List<Long> nexts = nodeI.getNexts();
            System.out.println(nexts);
            for (int j = 0; j < nexts.size(); j++) {
                nexts.set(j, keyHash.get(nexts.get(j)));
            }
            nodeI.setNexts(nexts);
            System.out.println(nexts);
            nodeI.nextsSerialize();
            nodeRepository.save(deserializedJourney.get(i));
            if (nodeI.getHeadOrTail() == 1) {//add to start list if the node is a start node
                heads.add(nodeI);
            }
        }
        //2.Start Journey from start node
        for (int i = 0; i < heads.size(); i++) {
            CoreModuleTask coreModuleTask = new CoreModuleTask();
            coreModuleTask.setNodeId(heads.get(i).getId());
            //Dummy Task
            coreModuleTask.setTargetNodeId(activeNodeRepository.findByDBNodeId(heads.get(i).getId()).getId());//Target node ->source
            cmtExecutor.execute(coreModuleTask);
        }
        return journey;
    }


    @Override
    public Boolean deleteActiveAudience(List<Long> activeNodeIdList) {
        try {
            activeAudienceRepository.deleteWhenEndNode(activeNodeIdList);
            System.out.println("删除 DeleteActiveAudience  成功");
            return true;
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(OutAPICaller.class);
            logger.error("DeleteActiveAudience error log:" + e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean deleteActiveNodeAndJourney(Long JourneyId) {
        try {
            ActiveJourney ActiveJourney = activeJourneyRepository.searchActiveJourneyByJourneyId(JourneyId);
            Long nodeJourneyId = ActiveJourney.getId();
            activeNodeRepository.deleteByNodeJourneyId(nodeJourneyId);
            System.out.println("删除 DeleteActiveNode nodeJourneyId:" + nodeJourneyId + " 成功");
            activeJourneyRepository.deleteByActiveJourneyId(JourneyId);
            System.out.println("删除 DeleteActiveJourney Id:" + JourneyId + " 成功");
            return true;
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(OutAPICaller.class);
            logger.error("DeleteActiveNode error log:" + e);
            return false;
        }
    }

    /**
     * end journey method
     * @param journeyId given journey id
     * @return
     */
    public Boolean endJourney(Long journeyId) {
        try {
            log.info("Prepare to end journey...");

            Optional<Journey> optionalJourney = journeyRepository.findById(journeyId);

            // if journey not found by given journey id, throw JourneyNotFoundException
            Journey journey = OptionalUtils.getObjectOrThrow(optionalJourney, "Jaudienceourney not found by given journey id!");

            ActiveJourney activeJourney = activeJourneyRepository.searchActiveJourneyByJourneyId(journeyId);
            List<Long> activeNodeIdList = new ArrayList<>();
            for (ActiveNode activeNode : activeNodeRepository.searchByNodeJourneyId(activeJourney.getId())) {
                activeNodeIdList.add(activeNode.getId());
            }

            // set journey status
            deleteActiveAudience(activeNodeIdList);
            deleteActiveNodeAndJourney(journeyId);
            log.info("set journey status from {} to {}", journey.getStatus(), Journey.ACTIVATED_FINISHED);
            journey.setStatus(Journey.ACTIVATED_FINISHED);
            // set updated properties
            LocalDateTime updateAt = LocalDateTime.now();
            journey.setUpdatedAt(updateAt);
            journey.setUpdatedBy("System");
            journeyRepository.save(journey);
            return true;
        }
        catch (Exception e) {
            log.info("End journey error");
            return false;
        }
    }
}
