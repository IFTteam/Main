package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.ParameterResolutionDelegate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveJourney;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.repository.JourneyRepository;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.activeRepository.ActiveJourneyRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;
import springredis.demo.serializer.SeDeFunction;
import springredis.demo.tasks.TaskExecutor;
import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@RestController
public class JourneyController {
    @Autowired
    private JourneyRepository journeyRepository;
    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ActiveJourneyRepository activeJourneyRepository;

    @Autowired
    private ActiveNodeRepository activeNodeRepository;
    @PostMapping("/journey/saveJourney")//保存Journey,仅仅保存Serialized部分
    public Journey saveJourney(@RequestBody String journeyJson){
        SeDeFunction sede = new SeDeFunction();
        // Map JourneyJson to JourneyJsonModel
        JourneyJsonModel journeyJsonModel = sede.deserializeJounrey(journeyJson);
        // Create Journey object using JourneyJson's info then store in DB
        Journey oneJourney = new Journey();
        oneJourney.setJourneySerialized(journeyJson);
        oneJourney.setJourneyName(journeyJsonModel.getProperties().getJourneyName());
//        LocalDateTime createAt = LocalDateTime.parse(journeyJsonModel.getCreatedAt(), DateTimeFormatter.ISO_DATE_TIME);
//        LocalDateTime updateAt = LocalDateTime.parse(journeyJsonModel.getUpdatedAt(), DateTimeFormatter.ISO_DATE_TIME);
//
//        oneJourney.setCreatedAt(createAt);
//        oneJourney.setCreatedBy(journeyJsonModel.getProperties().getCreatedBy());
//        oneJourney.setUpdatedAt(updateAt);
//        oneJourney.setUpdatedBy(journeyJsonModel.getProperties().getUpdatedBy());
        return journeyRepository.save(oneJourney);
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
                child1 = dfs(nodeJsonModelList[idx].getBranches().getTrue(), 0);
            }
            if (nodeJsonModelList[idx].getBranches().getFalse().length != 0) {
                child2 = dfs(nodeJsonModelList[idx].getBranches().getFalse(), 0);
            }
            if (child1 == null) {
                Node endNode = createEndNode();
                child1 = nodeRepository.save(endNode).getId();
                nodeIdList.add(endNode.getId());
            }
            if (child2 == null) {
                Node endNode = createEndNode();
                child2 = nodeRepository.save(endNode).getId();
                nodeIdList.add(endNode.getId());
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
                child = nodeRepository.save(endNode).getId();
                nodeIdList.add(endNode.getId());
            }
            nexts.add(child);
        }
        newNode.setNexts(nexts);
        nodeRepository.save(newNode);
        newNode = nodeRepository.searchNodeByid(nodeId);
        System.out.println("Name: " + newNode.getName() + "\nID: " + newNode.getId() + " \nChild:" + newNode.getNexts());
        return nodeId;
    }

    ArrayList<Long> nodeIdList = new ArrayList<>();
    private void createActiveNodesAndMapToNodes(ActiveJourney activeJourney) {
        for (int i = 0; i < nodeIdList.size(); i++) {
            ActiveNode activeNode = new ActiveNode();
            activeNode.setActiveJourney(activeJourney);
            activeNode.setNodeId(nodeIdList.get(i));
            activeNodeRepository.save(activeNode);
        }
    }
    @PostMapping("/journey/activateJourney")//激活Journey,查取数据库，反序列化
    public Journey activateJourney(@RequestBody String journeyJson){
        nodeIdList.clear();
        SeDeFunction sede = new SeDeFunction();
        // Map JourneyJson to JourneyJsonModel
        JourneyJsonModel journeyJsonModel = sede.deserializeJounrey(journeyJson);
        // Create Journey object using JourneyJson's info then store in DB
        Journey oneJourney = new Journey();
        oneJourney.setJourneySerialized(journeyJson);
        oneJourney.setJourneyName(journeyJsonModel.getProperties().getJourneyName());
        Long journeyid = journeyRepository.save(oneJourney).getId();


        // Traverse the journeyJsonModel object and add each node into DB
        dfs(journeyJsonModel.getSequence(), 0);
//        System.out.println(nodeIdList);

        // Create an ActiveJourney object and map to journey
        ActiveJourney activeJourney = new ActiveJourney();
        activeJourney.setJourneyId(journeyid);
        activeJourneyRepository.save(activeJourney);

        createActiveNodesAndMapToNodes(activeJourney);


        // set first node as head
        Node headNode = nodeRepository.searchNodeByid(nodeIdList.get(0));
        headNode.setHeadOrTail(1); // 1: root, 0: node, -1: leaf
        nodeRepository.save(headNode);
        return oneJourney;
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
        HashMap<Long,Long> keyHash = new HashMap<>();
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
            TaskExecutor taskExecutor = new TaskExecutor(coreModuleTask);
            taskExecutor.callModule(heads.get(i));
        }
        return journey;
    }
}