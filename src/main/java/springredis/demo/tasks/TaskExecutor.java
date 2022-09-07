package springredis.demo.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.activeEntity.ActiveNode;
//import springredis.demo.entity.otherModuleEntity.TimeTask;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveJourneyRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;

import java.util.Objects;
import java.util.Optional;

public class TaskExecutor implements Runnable {

    @Autowired
    RestTemplate restTemplate;

    private CoreModuleTask coreModuleTask;

    private ActiveAudienceRepository activeAudienceRepository;

    private ActiveNodeRepository activeNodeRepository;

    private ActiveJourneyRepository activeJourneyRepository;

    private NodeRepository nodeRepository;
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
        if (nextNode.isPresent()){
            callModule(nextNode.get());
        }else{
            System.out.println("target node not found");
        }





    }

    //call different apis at this point
    public void callModule(Node node) {
        String nodeType = node.getType();
        String nodeName = node.getName();
        String url = new String();

        if(Objects.equals(nodeType, "TimeDelay")){
//            TimeTask timeTask = new TimeTask();

        } else if (Objects.equals(nodeType, "If/Else")) {

        } else if (Objects.equals(nodeType, "API Trigger")) {
            HttpEntity<CoreModuleTask> call = new HttpEntity(coreModuleTask);
            //replace with server name
            url = "localhost:8080/API_trigger";
            CoreModuleTask response = restTemplate.exchange(url,HttpMethod.POST,call,CoreModuleTask.class).getBody();
        } else if (Objects.equals(nodeType, "SendEmail")) {

        } else if (Objects.equals(nodeType, "End")) {

        } else if (Objects.equals(nodeType, "Time Trigger")) {
            
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

    //
    private void moveUser(CoreModuleTask coreModuleTask) {
        Optional<ActiveAudience> activeAudience = activeAudienceRepository.findById(coreModuleTask.getActiveAudienceId());
        if (activeAudience.isPresent()){
            ActiveAudience activeAudience1 = activeAudience.get();
            activeAudience1.setActiveNode(activeNodeRepository.findById(coreModuleTask.getTargetNodeId()).get());
            activeAudienceRepository.save(activeAudience1);
        }

    }

    //checks whether active audience exist in
    private Boolean findAudience(Long id) {
        Optional<ActiveAudience> activeAudience = activeAudienceRepository.findById(id);
        return activeAudience.isPresent();
    }
}
