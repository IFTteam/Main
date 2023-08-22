package springredis.demo.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import springredis.demo.Service.JourneyService;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
import springredis.demo.error.DataBaseObjectNotFoundException;
import springredis.demo.repository.JourneyRepository;
import springredis.demo.repository.NodeRepository;
import springredis.demo.utils.OptionalUtils;

import java.util.*;

//takes a Core module task as parameter

@Component
@Slf4j
public class CMTExecutor {
    private final NodeRepository nodeRepository;

    private final JourneyRepository journeyRepository;

    @Autowired
    private JourneyService journeyService;


    RestTemplate restTemplate;
    //Chanage the below to actual API endpoints of functional urls
    private final HashMap<String, String> urlDict = new HashMap<>() {
        {
            put("Time Delay", "http://localhost:8080/Time_Delay");
            put("Place a Purchase", "http://localhost:8080/API_trigger");
            put("Abandon Checkout", "http://localhost:8080/API_trigger");
            put("Time Trigger", "http://localhost:8080/add");
            put("Send Email", "http://localhost:8080/actionSend/createCMTTransmission");
            put("If/Else", "http://localhost:8080/IfElse");
            put("Add Tag", "http://localhost:8080/AddTag");
            put("Remove Tag", "http://localhost:8080/RemoveTag");
            put("Subscribe", "http://localhost:8080/Subscribe"); //unknown
        }

    };

    @Autowired
    public CMTExecutor(NodeRepository nodeRepository, RestTemplate restTemplate,
                       JourneyRepository journeyRepository) {
        this.nodeRepository = nodeRepository;
        this.restTemplate = restTemplate;
        this.journeyRepository = journeyRepository;
    }


    public void execute(CoreModuleTask coreModuleTask) {
        System.out.println("The module to be execute is " + coreModuleTask);
        // find the curNode and dummyHeadNode in terms of coreModuleTask's NodeId
        Long nodeId = coreModuleTask.getNodeId();
        Optional<Node> optionalNode = nodeRepository.findById(nodeId);
        Node curNode = OptionalUtils.getObjectOrThrow(optionalNode, "Not found the node according to the given node id");
        Node dummyHeadNode = nodeRepository.searchByJourneyFrontEndIdAndName(curNode.getJourneyFrontEndId(), "dummyHead");
        Integer curEndNodesCount = curNode.getEndNodesCount();
        dummyHeadNode.nextsDeserialize();

        // find the trigger node and check if it is a time trigger
        Long triggerNodeId = dummyHeadNode.getNexts().get(0);
        Optional<Node> optionalTriggerNode = nodeRepository.findById(triggerNodeId);
        Node triggerNode = OptionalUtils.getObjectOrThrow(optionalTriggerNode, "Not found the node according to the given node id");

        boolean isTimeTrigger;
        isTimeTrigger = "Time Trigger".equals(triggerNode.getName());

        // first, if this coreModuleTask's type is "end", we change the journey status to ACTIVATED_FINISHED and return
        if ("end".equals(coreModuleTask.getType())) {
            if (isTimeTrigger) {
                // if it's belong to a time trigger journey, then delete the endNodesCount by 1
                dummyHeadNode.setEndNodesCount(dummyHeadNode.getEndNodesCount() - 1);
                nodeRepository.save(dummyHeadNode);
            }

            if (isTimeTrigger && dummyHeadNode.getEndNodesCount() == 0) {
                journeyService.endJourney(coreModuleTask.getJourneyId());
            }
            return;
        }

        // check activeAudienceId1 is empty
        if (!"dummyHead".equals(coreModuleTask.getName()) && coreModuleTask.getActiveAudienceId1().size() == 0) {
            if (isTimeTrigger) {
                dummyHeadNode.setEndNodesCount(dummyHeadNode.getEndNodesCount() - curEndNodesCount);
                nodeRepository.save(dummyHeadNode);
            }

            return;
        }

        CoreModuleTask restask = null;
        //else, we can first call the respective functional API's based on task type if the callapi attribute is 1:
        if (StringUtils.hasText(coreModuleTask.getName())) {
            System.out.println("The type of the task is:" + coreModuleTask.getName());
            System.out.println("The taskID is:" + coreModuleTask.getId());
            System.out.println("The getcallapi is:" + coreModuleTask.getCallapi());
        } else {
            System.out.println("The getcallapi is:" + coreModuleTask.getCallapi());
        }

        if (coreModuleTask.getCallapi() == 1) {
            restask = restTemplate.exchange(urlDict.get(coreModuleTask.getName()), HttpMethod.POST, new HttpEntity<>(coreModuleTask), CoreModuleTask.class).getBody();
        } else {
            restask = coreModuleTask;
        }
        //now, if restask.makenext is set to 0, the task executor will simply return since it won't do anything
        if (restask.getMakenext() == 0) {
            return;
        }
        //else: first move audience from curnode to next node, or create new active audience in nextnode
        //change local host to server domain!!
        //moving active audience pool from current node to next node (via method in task controller)
        if (restask.getTaskType() == 0) {
            restask = restTemplate.exchange("http://localhost:8080/move_user", HttpMethod.POST, new HttpEntity<>(restask), CoreModuleTask.class).getBody();
        } else {
            restask = restTemplate.exchange("http://localhost:8080/create_user", HttpMethod.POST, new HttpEntity<>(restask), CoreModuleTask.class).getBody();
            System.out.println("in CMT, after create user, the au is:" +
                    restask.getActiveAudienceId1());
        }
        System.out.println("restask node id: " + restask.getNodeId());
        Node curnode = nodeRepository.searchNodeByid(restask.getNodeId());
        curnode.nextsDeserialize();
        System.out.println("current node is: " + curnode.getName());
        System.out.println("the size of getNexts() of current node: " + curnode.getNexts().size());

        // finally, make and push new tasks based on next node
        for (int i = 0; i < curnode.getNexts().size(); i++) {
            System.out.println("++++++++++++++++get nexts is being excute");
            System.out.println("curnode.getNexts() is" + curnode.getNexts().toString());
            Long id = curnode.getNexts().get(i);
            Optional<Node> optionalNextNode = nodeRepository.findById(id);
            if (optionalNextNode.isEmpty()) {
                throw new DataBaseObjectNotFoundException("The Next node does not exist");
            }
            Node nextnode = optionalNextNode.get();
            nextnode.nextsDeserialize();
            CoreModuleTask newTask = new CoreModuleTask();
            BeanUtils.copyProperties(restask, newTask, "nodeId", "taskType", "type", "name", "sourceNodeId", "targetNodeId", "callapi", "activeAudienceId1", "activeAudienceId2", "audienceId1", "audienceId2");
            newTask.setNodeId(id);
            newTask.setTaskType(0);
            newTask.setType(nextnode.getType());
            newTask.setName(nextnode.getName());
            newTask.setSourceNodeId(nextnode.getId());
            if (i == 0) {
                newTask.setActiveAudienceId1(restask.getActiveAudienceId1());
                newTask.setAudienceId1(restask.getAudienceId1());
            }
            else {
                newTask.setActiveAudienceId1(restask.getActiveAudienceId2());
                newTask.setAudienceId1(restask.getAudienceId2());
            }
            if (nextnode.getNexts().size() > 0) {
                // this targetnodeid attribute is not really useful anymore
                newTask.setTargetNodeId(nodeRepository.searchNodeByid(nextnode.getNexts().get(0)).getId());
                // if nextnode has nexts, then newTask should make next node
                newTask.setMakenext(1);
            }
            String url = "http://localhost:8080/ReturnTask";
            HttpEntity<CoreModuleTask> httpEntity = new HttpEntity<>(newTask);
            // successfully pushed a new task by calling task controller (return task id if successful)
            Long taskid = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Long.class).getBody();
        }
    }

}
