package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

import springredis.demo.entity.Audience;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
import springredis.demo.entity.TimeTask;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.TimeDelayRepository;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;

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
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nodeRepository.searchNodeByid(nodeRepository.searchNodeByid(coreModuleTask.getNodeId()).getNexts().get(0)).getId());
                if (activeNode != null) {
                    ActiveAudience activeAudience = new ActiveAudience(Audid);
                    activeAudience.setActiveNode(activeNode);
                    activeAudience = activeAudienceRepository.save(activeAudience);
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
                }
            }
        }
        System.out.println("success");
        return 1L;
    }

    /**
     * 修改 {@link ActiveAudience#getActiveNode()}并保存到数据库
     * @param coreModuleTask
     * @return
     */
    @PostMapping("/move_user")
    private Long moveUser(@RequestBody CoreModuleTask coreModuleTask) {
        if (coreModuleTask.getAudienceId1().size() != 0) {                  //this means we have audience to create in the first connected node of the source node
            for (Long AudId : coreModuleTask.getAudienceId1()) {
                Node node = nodeRepository.searchNodeByid(coreModuleTask.getNodeId());
                Long nextNodeId = node.getNexts().get(0);
                nextNodeId = nodeRepository.searchNodeByid(nextNodeId).getId();
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nextNodeId);
                if (activeNode != null) {
                    ActiveAudience activeAudience = activeAudienceRepository.findByDBId(AudId);
                    activeAudience.setActiveNode(activeNode);
                    activeAudienceRepository.save(activeAudience);
                }
            }
        }
        if (coreModuleTask.getAudienceId2().size() != 0) {                  //this means we have audience to create in the second connected node of the source node
            for (Long AudId : coreModuleTask.getAudienceId2()) {
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nodeRepository.searchNodeByid(nodeRepository.searchNodeByid(coreModuleTask.getNodeId()).getNexts().get(1)).getId());
                if (activeNode!=null) {
                    ActiveAudience activeAudience = activeAudienceRepository.findByDBId(AudId);
                    activeAudience.setActiveNode(activeNode);
                    activeAudience = activeAudienceRepository.save(activeAudience);
                }
            }
        }
        System.out.println("success");
        return 1L;
    }
    @Autowired
    private TimeDelayRepository timeDelayRepository;
    @Autowired
    AudienceRepository audienceRepository;
    /**
     * 执行Brithday类型的任务
     * @param coreModuleTask
     * @return
     */
    @PostMapping("/Brithday")
    public List<TimeTask> Brithday(@RequestBody CoreModuleTask coreModuleTask){
        Long id = coreModuleTask.getId();
        Long nodeId = coreModuleTask.getNodeId();
        String type = coreModuleTask.getType();
        /**
         * active_node 表的id
         */
        Long targetNodeId = coreModuleTask.getTargetNodeId();

        List<ActiveAudience> activeAudienceList =  activeAudienceRepository.findByAudienceNodeId(targetNodeId);
        if (!CollectionUtils.isEmpty(activeAudienceList)) {
            for (ActiveAudience activeAudience : activeAudienceList) {
                // 用户id,就是给这个人发送的生日邮件.
                Long audienceId = activeAudience.getAudienceId();
                Audience audience = audienceRepository.searchAudienceByid(audienceId);
                Date birthday = audience.getBirthday();

                TimeTask x = new TimeTask();
                x.setTriggerTime(birthday.getTime());// todo:只有年月日没有时分秒,也就是会00:00:00给audience发送邮件.
                x.setRepeatTimes(1);// todo:
                x.setRepeatInterval("");// todo:
                x.setTaskStatus(0);// 状态:在数据库中
                // 把timeTask保存到 time_task 表中.
                x.setCreatedAt(LocalDateTime.now());
                x.setCreatedBy("BrithdayTask");
                timeDelayRepository.save(x);
                //  todo:疑问:时间延迟之后,如何设置下一个发送邮件的节点?nextNodeId.
//                   生日 类型的任务执行时，会隐式生成一个TimeTask Node，       A -> timeTask -> B
//                   所以需要修改node表中的数据。
//                   第一个节点是：active_node.node_id
//                   第二个节点是，程序中自动生成的延迟节点。
//                   x = new timeTask
//                   BNodeId = a.next
//                   A.next = x.NodeId
//                   x.next = BNodeId




            }
        }


        return null;
    }

}
