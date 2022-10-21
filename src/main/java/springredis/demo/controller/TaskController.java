package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;

import java.util.Optional;

//the API controller to receive task from other module
@RestController
public class TaskController {
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    ActiveAudienceRepository activeAudienceRepository;
    @Autowired
    ActiveNodeRepository activeNodeRepository;
    @Autowired
    NodeRepository nodeRepository;


    private final String taskQueueKey = "CoretaskQueue";
    @PostMapping("/ReturnTask")
    public Long addTask(@RequestBody CoreModuleTask coreModuleTask){

        return redisTemplate.opsForList().leftPush(taskQueueKey ,coreModuleTask);
    }

    //returns the created user's id in buffer (not main DB!!)
    @PostMapping("/create_user")
    private Long createUser(@RequestBody CoreModuleTask coreModuleTask) {
        if (coreModuleTask.getAudienceId1().size() != 0) {                  //this means we have audience to create in the first connected node of the source node
            for (Long Audid : coreModuleTask.getAudienceId1()) {
                ActiveAudience activeAudience = new ActiveAudience(Audid);
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nodeRepository.searchNodeByid(nodeRepository.searchNodeByid(coreModuleTask.getNodeId()).getNexts().get(0)).getId());
                if (activeNode != null) {
                    activeAudience.setActiveNode(activeNode);
                    activeAudience = activeAudienceRepository.save(activeAudience);
                    return activeAudience.getId();
                }
            }
        }
        if (coreModuleTask.getAudienceId2().size() != 0) {                  //this means we have audience to create in the second connected node of the source node
            for (Long Audid : coreModuleTask.getAudienceId2()) {
                ActiveAudience activeAudience = new ActiveAudience(Audid);
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nodeRepository.searchNodeByid(nodeRepository.searchNodeByid(coreModuleTask.getNodeId()).getNexts().get(1)).getId());
                if (activeNode!=null) {
                    activeAudience.setActiveNode(activeNode);
                    activeAudience = activeAudienceRepository.save(activeAudience);
                    return activeAudience.getId();
                }
            }
        }
        System.out.println("Create ActiveAudience Error");
        return -1L;
    }

    @PostMapping("/move_user")
    private Long moveUser(@RequestBody CoreModuleTask coreModuleTask) {
        if (coreModuleTask.getAudienceId1().size() != 0) {                  //this means we have audience to create in the first connected node of the source node
            for (Long AudId : coreModuleTask.getAudienceId1()) {
                ActiveAudience activeAudience = activeAudienceRepository.findByDBId(AudId);
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nodeRepository.searchNodeByid(nodeRepository.searchNodeByid(coreModuleTask.getNodeId()).getNexts().get(0)).getId());
                if (activeNode != null) {
                    activeAudience.setActiveNode(activeNode);
                    activeAudience = activeAudienceRepository.save(activeAudience);
                    return activeAudience.getId();
                }
            }
        }
        if (coreModuleTask.getAudienceId2().size() != 0) {                  //this means we have audience to create in the second connected node of the source node
            for (Long AudId : coreModuleTask.getAudienceId1()) {
                ActiveAudience activeAudience = activeAudienceRepository.findByDBId(AudId);
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nodeRepository.searchNodeByid(nodeRepository.searchNodeByid(coreModuleTask.getNodeId()).getNexts().get(1)).getId());
                if (activeNode!=null) {
                    activeAudience.setActiveNode(activeNode);
                    activeAudience = activeAudienceRepository.save(activeAudience);
                    return activeAudience.getId();
                }
            }
        }
        System.out.println("Create ActiveAudience Error");
        return -1L;
    }

}
