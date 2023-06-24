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
import springredis.demo.entity.Journey;
import springredis.demo.entity.Node;
import springredis.demo.error.JourneyNotFoundException;
import springredis.demo.repository.JourneyRepository;
import springredis.demo.repository.NodeRepository;

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
            put("API Trigger", "http://localhost:8080/API_trigger");
            put("Time Trigger", "http://localhost:8080/Time_Trigger");
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
        // first, if this coremoduletask's type is "end", we change the journey status to ACTIVATED_FINISHED and return
        if ("end".equals(coreModuleTask.getType())) {
            log.info("Prepare to end journey...");
            Long journeyId = coreModuleTask.getJourneyId();
            Optional<Journey> optionalJourney = journeyRepository.findById(journeyId);

            // if journey not found by given journey id, throw JourneyNotFoundException
            if (optionalJourney.isEmpty()) {
                throw new JourneyNotFoundException("Journey not found by given journey id!");
            }

            // set journey status
            Journey journey = optionalJourney.get();
            journeyService.deleteActiveAudience(coreModuleTask.getActiveAudienceId1().get(0));
            journeyService.deleteActiveNodeAndJourney(journeyId);
            journey.setStatus(Journey.ACTIVATED_FINISHED);
            journeyRepository.save(journey);
            log.info("set journey status from {} to {}", journey.getStatus(), Journey.ACTIVATED_FINISHED);

            // end execute
            return;
        }
        CoreModuleTask restask = null;
        //else, we can first call the respective functional API's based on task type if the callapi attribute is 1:
        System.out.println("The task is:" + coreModuleTask.toString());
        if (StringUtils.hasText(coreModuleTask.getName())) {
            System.out.println("The type of the task is:" + coreModuleTask.getName());
            System.out.println("The taskID is:" + coreModuleTask.getId());
            System.out.println("The getcallapi is:" + coreModuleTask.getCallapi());
        } else {
            System.out.println("The getcallapi is:" + coreModuleTask.getCallapi());
        }
        //System.out.println("The url is:" + urlDict.get(coreModuleTask.getType()).toString());
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
        Long activeid;
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

        //finally, make and push new tasks based on next node
        for (int i = 0; i < curnode.getNexts().size(); i++) {
            System.out.println("++++++++++++++++get nexts is being excute");
            System.out.println("curnode.getNexts() is" + curnode.getNexts().toString());
            Long id = curnode.getNexts().get(i);
            Node nextnode = nodeRepository.searchNodeByid(id);
            nextnode.nextsDeserialize();
            CoreModuleTask newTask = new CoreModuleTask();
            BeanUtils.copyProperties(restask, newTask, "nodeId", "taskType", "type", "name", "sourceNodeId", "targetNodeId", "callapi");
            newTask.setNodeId(id);
            newTask.setTaskType(0);
            newTask.setType(nextnode.getType());
            newTask.setName(nextnode.getName());
            newTask.setSourceNodeId(nextnode.getId());
            if (nextnode.getNexts().size() > 0) {
                newTask.setTargetNodeId(nodeRepository.searchNodeByid(nextnode.getNexts().get(0)).getId());         //this targetnodeid attribute is not really useful anymore
            }
            //now we identify the current activeNode
//            ActiveNode activeNode = activeNodeRepository.findByDBNodeId(id);
//            Node node = nodeRepository.searchNodeByid(id);
//            List<ActiveAudience> activeAudienceList = activeNode.getActiveAudienceList();                       //since the corresponding active audience pool for the possible if/else nextnode is already taken care of in move audience, we simply assign the active audience list to the first AAL attribute of the node's CMT
//            List<Long> activeIDs = new ArrayList<>();
//            List<Long> IDs = new ArrayList<>();
            /*for (ActiveAudience aud : activeAudienceList) {
                activeIDs.add(aud.getId());
                IDs.add(aud.getAudienceId());
            }*/
            String url = "http://localhost:8080/ReturnTask";
            HttpEntity<CoreModuleTask> httpEntity = new HttpEntity<>(newTask);
            Long taskid = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Long.class).getBody();              //successfully pushed a new task by calling task controller (return task id if successful)
        }
    }
}
