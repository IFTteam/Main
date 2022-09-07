package springredis.demo.tasks;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
        restTemplate.postForObject(urlDict.get(nodeType), baseTaskEntity, String.class);



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

    private void moveUser(CoreModuleTask coreModuleTask) {
        Optional<ActiveAudience> activeAudience = activeAudienceRepository.findById(coreModuleTask.getActiveAudienceId());
        if (activeAudience.isPresent()){
            ActiveAudience activeAudience1 = activeAudience.get();
            activeAudience1.setActiveNode(activeNodeRepository.findById(coreModuleTask.getTargetNodeId()).get());
            activeAudienceRepository.save(activeAudience1);
        }

    }

    private Boolean findAudience(Long id) {
        Optional<ActiveAudience> activeAudience = activeAudienceRepository.findById(id);
        return activeAudience.isPresent();
    }
}