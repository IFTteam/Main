package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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

    @PostMapping("/move_user")
    private Long moveUser(@RequestBody CoreModuleTask coreModuleTask) {
        if (coreModuleTask.getAudienceId1().size() != 0) {                  //this means we have audience to create in the first connected node of the source node
            for (Long AudId : coreModuleTask.getAudienceId1()) {
                ActiveAudience activeAudience = activeAudienceRepository.findByDBId(AudId);
                ActiveNode activeNode = activeNodeRepository.findByDBNodeId(nodeRepository.searchNodeByid(nodeRepository.searchNodeByid(coreModuleTask.getNodeId()).getNexts().get(0)).getId());
                if (activeNode != null) {
                    activeAudience.setActiveNode(activeNode);
                    activeAudience = activeAudienceRepository.save(activeAudience);
                }
            }
        }
        if (coreModuleTask.getAudienceId2().size() != 0) {                  //this means we have audience to create in the second connected node of the source node
            for (Long AudId : coreModuleTask.getAudienceId2()) {
                ActiveAudience activeAudience = activeAudienceRepository.findByDBId(AudId);
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

    @Autowired
    private TimeDelayRepository timeDelayRepository;
    @Autowired
    AudienceRepository audienceRepository;

    /**
     * 执行Birthday类型的任务
     * @param coreModuleTask
     * @return
     */
    @PostMapping("/Birthday")
    public List<TimeTask> Birthday(@RequestBody CoreModuleTask coreModuleTask){
        Long id = coreModuleTask.getId();
        Long nodeId = coreModuleTask.getNodeId();
        String type = coreModuleTask.getType();
        /**
         * active_node 表的id
         */
        Long activeNodeId = coreModuleTask.getTargetNodeId();
        List<ActiveAudience> activeAudienceList =  activeAudienceRepository.findByAudienceNodeId(activeNodeId);
        ActiveNode activeNode = activeNodeRepository.findById(activeNodeId).orElse(null);

        if (!CollectionUtils.isEmpty(activeAudienceList)) {
            for (ActiveAudience activeAudience : activeAudienceList) {
                // 用户id,就是给这个人发送的生日邮件.
                Long audienceId = activeAudience.getAudienceId();
                Audience audience = audienceRepository.searchAudienceByid(audienceId);
                Date birthday = audience.getBirthday();

                Node a = nodeRepository.searchNodeByid(nodeId);
                a.nextsDeserialize();
                Long bNodeId = a.getNexts().get(0);
                Node b = nodeRepository.searchNodeByid(bNodeId);


                // 创建一个新的node
//                   所以需要修改node表中的数据。
//                   第一个节点是：active_node.node_id
//                   第二个节点是，程序中自动生成的延迟节点。
//                   x = new Node()
//                   BNodeId = a.next
//                   A.next = x.NodeId
//                   x.next = BNodeId
                // todo:疑问:这里添加了一个node,但是这个node需要做的事情,是在哪里设置的?比如发邮件就需要和campaign表进行关联.这两个表是如何关联的?
                Node x = new Node();
                x.setType("TimeDelay");
                x.setName("TimeDelay");
                x.setStatus(a.getStatus());
                x.setCreatedAt(LocalDateTime.now());
                x.setCreatedBy("BirthdayTask");

                x.setNexts(Collections.singletonList(bNodeId));
                nodeRepository.save(x);
                a.setNexts(Collections.singletonList(x.getId()));
                nodeRepository.save(a);
                nodeRepository.save(b);


                // 根据journeyId,查询 active_journey
                ActiveNode newActiveNode = new ActiveNode();
                newActiveNode.setNodeId(x.getId());
                ActiveAudience o = new ActiveAudience();
                o.setAudienceId(audienceId);
                o.setActiveNode(newActiveNode);
                newActiveNode.setActiveAudienceList(Collections.singletonList(o));
                activeNodeRepository.save(newActiveNode);


                // 最后创建一个timeTask放到数据库中.
                TimeTask brithdayTimeTask = new TimeTask();
                brithdayTimeTask.setTriggerTime(birthday.getTime());// todo:只有年月日没有时分秒,也就是会00:00:00给audience发送邮件.
                brithdayTimeTask.setRepeatTimes(1);// todo:重复次数
                brithdayTimeTask.setRepeatInterval("1Y");// 重复间隔:1年
                brithdayTimeTask.setTaskStatus(0);// 状态:在数据库中
                // 把timeTask保存到 time_task 表中.
                brithdayTimeTask.setCreatedAt(LocalDateTime.now());
                brithdayTimeTask.setCreatedBy("BirthdayTask");
                timeDelayRepository.save(brithdayTimeTask);
            }
        }


        return null;
    }

}
