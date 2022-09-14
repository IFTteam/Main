package springredis.demo.tasks;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveJourneyRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;

import javax.swing.text.html.HTML;
import java.util.*;

public class TaskExecutor implements Runnable {
    private CoreModuleTask coreModuleTask;

    private ActiveAudienceRepository activeAudienceRepository;

    private ActiveNodeRepository activeNodeRepository;

    private ActiveJourneyRepository activeJourneyRepository;

    private NodeRepository nodeRepository;
    private RestTemplate restTemplate = new RestTemplate();
    private HashMap<String,String> urlDict = new HashMap<String,String>(){
        {
            put("TimeDelay","http://localhost:3000");
            put("APITrigger","http://localhost:3001");
            put("End","http://localhost:3002");
            put("TimeTrigger","http://localhost:3003");
            put("SendEmail","http://localhost:3004");

        }

    };

    public TaskExecutor(CoreModuleTask coreModuleTask) {
        this.coreModuleTask = coreModuleTask;
    }

    public TaskExecutor(CoreModuleTask coreModuleTask, ActiveAudienceRepository activeAudienceRepository, ActiveNodeRepository activeNodeRepository, ActiveJourneyRepository activeJourneyRepository, NodeRepository nodeRepository) {
        this.coreModuleTask = coreModuleTask;
        this.activeAudienceRepository = activeAudienceRepository;
        this.activeNodeRepository = activeNodeRepository;
        this.activeJourneyRepository = activeJourneyRepository;
        this.nodeRepository = nodeRepository;
    }

    @Override
    public void run() {
        //move or create audience on audience buffer
        if (coreModuleTask.getTaskType()==0){
            if (findAudience(coreModuleTask.getActiveAudienceId())){//User found and move from one buffer to another
                moveUser(coreModuleTask);
            }
            else{//No user found, return to thread pool
                return;
            }

        }else{//Directly create user in the next buffer
            coreModuleTask.setAudienceId(createUser(coreModuleTask));

        }

        //call API
        Optional<Node> nextNode = nodeRepository.findById(coreModuleTask.getTargetNodeId());
        if (nextNode.isPresent() && !Objects.equals( nextNode.get().getName(),"End")){
            callModule(nextNode.get());
        }else{
            System.out.println("target node not found");
        }





    }

    public void callModule(Node node) {
        String nodeType = node.getType();
        String nodeName = node.getName();
        BaseTaskEntity baseTaskEntity = coreModuleTask;
//        baseTaskEntity.setJourneyId(coreModuleTask.getJourneyId());
//        baseTaskEntity.setNodeId(coreModuleTask.getNodeId());
//        baseTaskEntity.setUserId(coreModuleTask.getUserId());
        // switch the previous target node to new source node
        baseTaskEntity.setSourceNodeId(coreModuleTask.getTargetNodeId());
        // get new target node
        if ((node.getNexts()).size()>0){
            baseTaskEntity.setTargetNodeId(activeNodeRepository.findByDBNodeId((node.getNexts()).get(0)).getId());
        }
        //Call module accordingly
        CoreModuleTask curtask = restTemplate.postForObject(urlDict.get(nodeType), baseTaskEntity, CoreModuleTask.class);
        Long activeaudienceid;
        //first, identify whether the task specifies to move or create an audience in the active respostiory
        if(curtask.getTaskType()==0){activeaudienceid=moveUser(curtask);}
        else activeaudienceid=createUser(curtask);
        //then, query for the next node of the current task's node; make sure. Finally, call task controller to add new task to queue
        Node curnode = nodeRepository.searchNodeByid(curtask.getNodeId());
        //push a new task for each active audience in the next node's activeaudience pool; note that each activeaudience must be already saved in main DB (have corresponding audienceid)
        for(long id:curnode.getNexts()){
            Node nextnode = nodeRepository.searchNodeByid(id);
            ActiveNode activeNode = activeNodeRepository.findByDBNodeId(id);
            List<ActiveAudience> activeAudienceList = activeNode.getActiveAudienceList();
            for(ActiveAudience activeAudience:activeAudienceList){
                CoreModuleTask newtask = new CoreModuleTask();
                newtask.setTaskType(0);                 //all tasks are move-user except for trigger hit, which is not taken care of here
                newtask.setType(nextnode.getType());
                newtask.setName(nextnode.getName());
                newtask.setSourceNodeId(nextnode.getId());
                newtask.setTargetNodeId(nodeRepository.searchNodeByid(nextnode.getNexts().get(0)).getId());
                newtask.setActiveAudienceId(activeaudienceid);
                newtask.setNodeId(nextnode.getId()); newtask.setUserId(curtask.getUserId()); newtask.setJourneyId(curtask.getJourneyId());
                newtask.setAudienceId(activeAudience.getAudienceId());
                //replace domain name with server domain
                String url = "https://localhost:8080/ReturnTask";
                HttpEntity<CoreModuleTask> httpEntity = new HttpEntity<>(newtask);
                Long taskid = restTemplate.exchange(url, HttpMethod.POST,httpEntity,Long.class).getBody();              //successfully pushed a new task by calling task controller (return task id if successful)
            }
        }

    }

    private Long createUser(CoreModuleTask coreModuleTask) {
        ActiveAudience activeAudience = new ActiveAudience(coreModuleTask.getAudienceId());
        Optional<ActiveNode> activeNode = activeNodeRepository.findById(coreModuleTask.getTargetNodeId());
        if (activeNode.isPresent()){
            activeAudience.setActiveNode(activeNode.get());
            activeAudience = activeAudienceRepository.save(activeAudience);
            return activeAudience.getId();

        }
        System.out.println("Create ActiveAudience Error");
        return -1L;

    }

    private Long moveUser(CoreModuleTask coreModuleTask) {
        Optional<ActiveAudience> activeAudience = activeAudienceRepository.findById(coreModuleTask.getActiveAudienceId());
        if (activeAudience.isPresent()){
            ActiveAudience activeAudience1 = activeAudience.get();
            activeAudience1.setActiveNode(activeNodeRepository.findById(coreModuleTask.getTargetNodeId()).get());
            return activeAudienceRepository.save(activeAudience1).getId();
        }
        return -1L;
    }

    private Boolean findAudience(Long id) {
        Optional<ActiveAudience> activeAudience = activeAudienceRepository.findById(id);
        return activeAudience.isPresent();
    }
}