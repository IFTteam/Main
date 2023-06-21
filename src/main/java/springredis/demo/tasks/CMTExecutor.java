package springredis.demo.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import springredis.demo.Service.DAO;
import springredis.demo.controller.JourneyController;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Journey;
import springredis.demo.entity.Node;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.error.JourneyNotFoundException;
import springredis.demo.repository.JourneyRepository;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveJourneyRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;
import springredis.demo.controller.JourneyController;
import java.util.*;

//takes a Core module task as parameter

@Component
@Slf4j
public class CMTExecutor{
    @Autowired
    private JourneyRepository journeyRepository;

    private ActiveAudienceRepository activeAudienceRepository;

    private ActiveNodeRepository activeNodeRepository;

    private ActiveJourneyRepository activeJourneyRepository;

    private NodeRepository nodeRepository;
    @Autowired
    JourneyController journeyController;

    RestTemplate restTemplate;
    //Chanage the below to actual API endpoints of functional urls
    private HashMap<String, String> urlDict = new HashMap<String, String>() {
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
    public CMTExecutor(NodeRepository nodeRepository, RestTemplate restTemplate, ActiveNodeRepository activeNodeRepository){
        this.nodeRepository = nodeRepository;
        this.restTemplate = restTemplate;
        this.activeNodeRepository = activeNodeRepository;
    }

    public CMTExecutor(CoreModuleTask coreModuleTask, ActiveAudienceRepository activeAudienceRepository, ActiveNodeRepository activeNodeRepository, ActiveJourneyRepository activeJourneyRepository, NodeRepository nodeRepository) {
        this.activeAudienceRepository = activeAudienceRepository;
        this.activeNodeRepository = activeNodeRepository;
        this.activeJourneyRepository = activeJourneyRepository;
        this.nodeRepository = nodeRepository;
    }

    public void execute(CoreModuleTask coreModuleTask) {
        System.out.println("The module to be execute is " + coreModuleTask);
        // first, if this coremoduletask's type is "end", we change the journey status to ACTIVATED_FINISHED and return
        if ("end".equals(coreModuleTask.getType())) {
            log.info("Prepare to end journey...");
            // set journey status to end

            // get journey id and find in DB
            Long journeyId = coreModuleTask.getJourneyId();
            Optional<Journey> optionalJourney = journeyRepository.findById(journeyId);

            // if journey not found by given journey id, throw JourneyNotFoundException
            if (optionalJourney.isEmpty()) {
                throw new JourneyNotFoundException("Journey not found by given journey id!");
            }

            // set journey status
            Journey journey = optionalJourney.get();
            log.info("set journey status from {} to {}", journey.getStatus(), JourneyController.ACTIVATED_FINISHED);
            Long JourneyId = coreModuleTask.getJourneyId();
            journeyController.DeleteActiveAudience(coreModuleTask.getActiveAudienceId1().get(0));
            journeyController.DeleteActiveNodeAndJourney(JourneyId);
            journey.setStatus(JourneyController.ACTIVATED_FINISHED);
            journeyRepository.save(journey);

            // end execute
            return;
        }
        CoreModuleTask restask = null;
        //else, we can first call the respective functional API's based on task type if the callapi attribute is 1:
        System.out.println("The task is:" + coreModuleTask.toString());
        if (coreModuleTask.getName() != null) {
            System.out.println("The type of the task is:" + coreModuleTask.getName().toString());
            System.out.println("The taskID is:" + coreModuleTask.getId());
            System.out.println("The getcallapi is:" + coreModuleTask.getCallapi());
        }
        else {
            System.out.println("The getcallapi is:" + coreModuleTask.getCallapi());
        }
        //System.out.println("The url is:" + urlDict.get(coreModuleTask.getType()).toString());
        if (coreModuleTask.getCallapi() == 1) {
            restask = restTemplate.exchange(urlDict.get(coreModuleTask.getName()), HttpMethod.POST, new HttpEntity<>(coreModuleTask), CoreModuleTask.class).getBody();
        }
        else {
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
        System.out.println("the size of getNexts() of current node: "+ curnode.getNexts().size());

        //finally, make and push new tasks based on next node
        for (int i = 0; i < curnode.getNexts().size(); i++) {
            System.out.println("++++++++++++++++get nexts is being excute");
            System.out.println("curnode.getNexts() is" + curnode.getNexts().toString());
            Long id = curnode.getNexts().get(i);
            Node nextnode = nodeRepository.searchNodeByid(id);
            nextnode.nextsDeserialize();
            CoreModuleTask newtask = new CoreModuleTask();
            newtask.setUserId(restask.getUserId());
            newtask.setJourneyId(restask.getJourneyId());
            newtask.setNodeId(id);
            newtask.setTaskType(0);
            newtask.setType(nextnode.getType());
            newtask.setName(nextnode.getName());
            newtask.setSourceNodeId(nextnode.getId());
            newtask.setActiveAudienceId1(restask.getActiveAudienceId1());
            newtask.setActiveAudienceId2(restask.getActiveAudienceId2());
            newtask.setAudienceId1(restask.getAudienceId1());
            newtask.setAudienceId2(restask.getAudienceId2());
            if(nextnode.getNexts().size()>0) {
                newtask.setTargetNodeId(nodeRepository.searchNodeByid(nextnode.getNexts().get(0)).getId());         //this targetnodeid attribute is not really useful anymore
            }
            //now we identify the current activeNode
            ActiveNode activeNode = activeNodeRepository.findByDBNodeId(id);
            Node node = nodeRepository.searchNodeByid(id);
            List<ActiveAudience> activeAudienceList = activeNode.getActiveAudienceList();                       //since the corresponding active audience pool for the possible if/else nextnode is already taken care of in move audience, we simply assign the active audience list to the first AAL attribute of the node's CMT
            List<Long> activeIDs = new ArrayList<>();
            List<Long> IDs = new ArrayList<>();
            /*for (ActiveAudience aud : activeAudienceList) {
                activeIDs.add(aud.getId());
                IDs.add(aud.getAudienceId());
            }*/
            newtask.setActiveAudienceId1(restask.getActiveAudienceId1());
            newtask.setAudienceId1(restask.getAudienceId1());
            String url = "http://localhost:8080/ReturnTask";
            HttpEntity<CoreModuleTask> httpEntity = new HttpEntity<>(newtask);
            Long taskid = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Long.class).getBody();              //successfully pushed a new task by calling task controller (return task id if successful)
        }
    }
}
