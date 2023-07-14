package springredis.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;
import springredis.demo.utils.OptionalUtils;

import java.util.List;
import java.util.Optional;

//the API controller to receive task from other module
@RestController
@Slf4j
public class TaskController {
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    ActiveAudienceRepository activeAudienceRepository;
    @Autowired
    ActiveNodeRepository activeNodeRepository;
    @Autowired
    NodeRepository nodeRepository;

    @Value("${redis-key.task-queue-key}")
    private String taskQueueKey;

    @PostMapping("/ReturnTask")
    public Long addTask(@RequestBody CoreModuleTask coreModuleTask) {

        return redisTemplate.opsForList().leftPush(taskQueueKey, coreModuleTask);
    }

    //returns the created user's id in buffer (not main DB!!)
    @PostMapping("/create_user")
    private CoreModuleTask createUser(@RequestBody CoreModuleTask coreModuleTask) {
        log.info("begin to create audience...");
        // create user should not happen in if else
        if (coreModuleTask.getAudienceId1() != null) {
            for (Long audienceId : coreModuleTask.getAudienceId1()) {
                log.info("prepare to create new activeAudience according to audienceId: {} from audienceId1", audienceId);
                ActiveAudience activeAudience = new ActiveAudience(audienceId);
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nodeRepository.searchNodeByid(nodeRepository.searchNodeByid(coreModuleTask.getNodeId()).getNexts().get(0)).getId());
                if (activeNode != null) {
                    activeAudience.setActiveNode(activeNode);
                    activeAudience = activeAudienceRepository.save(activeAudience);
                    //add the active-audience to core module attributes
                    List<Long> activeaudiencelist = coreModuleTask.getActiveAudienceId1();
                    activeaudiencelist.add(activeAudience.getId());
                    coreModuleTask.setActiveAudienceId1(activeaudiencelist);
                }
            }
            log.info("created new activeAudienceId1 done!");
            log.info("now the activeAudienceId1 is {}", coreModuleTask.getActiveAudienceId1());
        }
        log.info("created user successfully!");
        return coreModuleTask;
    }

    @PostMapping("/move_user")
    private CoreModuleTask moveUser(@RequestBody CoreModuleTask coreModuleTask) {
        log.info("begin to move audience...");
        if (coreModuleTask.getAudienceId1() != null && coreModuleTask.getAudienceId1().size() != 0) {
            //this means we have audience to create in the first connected node of the source node
            for (Long activeAudienceId : coreModuleTask.getActiveAudienceId1()) {
                log.info("prepare to move activeAudience: {} from activeAudienceId1", activeAudienceId);
                Optional<ActiveAudience> optionalActiveAudience = activeAudienceRepository.findById(activeAudienceId);
                ActiveAudience activeAudience = OptionalUtils.getObjectOrThrow(optionalActiveAudience, "Not found ActiveAudience by its id");
                // get next active node according to the next node of the current node
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nodeRepository.searchNodeByid(nodeRepository.searchNodeByid(coreModuleTask.getNodeId()).getNexts().get(0)).getId());
                if (activeNode != null) {
                    activeAudience.setActiveNode(activeNode);
                    activeAudienceRepository.save(activeAudience);
                }
            }
            log.info("moved activeAudienceId1 done!");
            log.info("now the activeAudienceId1 is {}", coreModuleTask.getActiveAudienceId1());
        }

        if (coreModuleTask.getAudienceId2() != null && coreModuleTask.getAudienceId2().size() != 0) {
            //this means we have audience to create in the first connected node of the source node
            for (Long activeAudienceId : coreModuleTask.getActiveAudienceId2()) {
                log.info("prepare to move activeAudience: {} from activeAudienceId2", activeAudienceId);
                Optional<ActiveAudience> optionalActiveAudience = activeAudienceRepository.findById(activeAudienceId);
                ActiveAudience activeAudience = OptionalUtils.getObjectOrThrow(optionalActiveAudience, "Not found ActiveAudience by its id");
                // get next active node according to the next node of the current node
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nodeRepository.searchNodeByid(nodeRepository.searchNodeByid(coreModuleTask.getNodeId()).getNexts().get(1)).getId());
                if (activeNode != null) {
                    activeAudience.setActiveNode(activeNode);
                    activeAudienceRepository.save(activeAudience);
                }
            }
            log.info("moved activeAudienceId2 done!");
            log.info("now the activeAudienceId2 is {}", coreModuleTask.getActiveAudienceId1());
        }
        log.info("moved user successfully!");
        return coreModuleTask;
    }

}
