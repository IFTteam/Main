package springredis.demo.structures;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Event;
import springredis.demo.entity.Node;
import springredis.demo.entity.TimeTask;
import springredis.demo.error.DataBaseObjectNotFoundException;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.TimeDelayRepository;
import springredis.demo.tasks.CMTExecutor;
import springredis.demo.utils.OptionalUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class OutAPICaller {

    private final TimeDelayRepository timeDelayRepository;

    private final NodeRepository nodeRepository;

    private final RedisTemplate redisTemplate;

    /**
     * auto-wired taskScheduler to do scheduling task
     */
    private final TaskScheduler taskScheduler;

    @Autowired
    private RestTemplate restTemplate = new RestTemplate();

    private final CMTExecutor cmtExecutor;

    @Value("${redis-key.out-queue-key}")
    private String outQueueKey;

    private ScheduledFuture<?> scheduledFuture;

    /**
     * corn Expression to determine current running status
     */
    @Setter
    @Getter
    @Value("${corn.expressions.everySecond}")
    private String cronExpression;


    @Autowired
    public OutAPICaller(TimeDelayRepository timeDelayRepository, RedisTemplate redisTemplate,
                        NodeRepository nodeRepository, CMTExecutor cmtExecutor, TaskScheduler taskScheduler) {
        this.timeDelayRepository = timeDelayRepository;
        this.redisTemplate = redisTemplate;
        this.nodeRepository = nodeRepository;
        this.cmtExecutor = cmtExecutor;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        log.info("start OutAPICaller...");
        startOutAPICaller(cronExpression);
    }

    /**
     * start processOutAPICaller() with given cronExpression
     * @param cronExpression given cronExpression
     */
    public void startOutAPICaller(String cronExpression) {
        log.info("start running OutAPICaller for {} ...", cronExpression);
        Runnable task = this::processOutAPICaller;
        setCronExpression(cronExpression);
        scheduledFuture = taskScheduler.schedule(task, new CronTrigger(cronExpression));
    }

    /**
     * stop processOutAPICaller()
     */
    @PreDestroy
    public void stopOutAPICaller() {
        log.info("stop running OutAPICaller...");
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(true);
        }
    }


    /**
     * This will be executed every 1 second according to cronExpression
     * while outQueueKey.size() > 0, then pop from outQueueKey and execute
     * the event
     */
    @SneakyThrows
    public void processOutAPICaller() {
        while (redisTemplate.opsForList().size(outQueueKey) > 0) {
            System.out.println("========== (OutAPICaller) Event detected in outQueue ========");
            Event outEvent = ((Event) redisTemplate.opsForList().rightPop(outQueueKey));
            Long id = ((Number) outEvent.getId()).longValue();
            System.out.println("the id is: " + id);
            Optional<TimeTask> timeTaskOp = timeDelayRepository.findById(id);
            TimeTask timeTask = OptionalUtils.getObjectOrThrow(timeTaskOp, "No Time Task Exist");
            timeTask.audience_serialize();
            System.out.println("================================================Time Task retrieved=====================================================");
            System.out.println("cur JI id is:" + timeTask.getJourneyId());
            System.out.println("Time Task Node: " + timeTask);
            System.out.println("================================================Time Trigger node retrieved=====================================================");
            // prepare a new cmt corresponding to dummy time task to execute
            CoreModuleTask coreModuleTask = createDummyCMTFromDummyTimeNode(timeTask);
            cmtExecutor.execute(coreModuleTask);
        }
    }

    private Node initializeNodeFromDB(Optional<Node> nodeOp) throws DataBaseObjectNotFoundException {
        if (nodeOp.isEmpty()) {
            throw new DataBaseObjectNotFoundException("The corresponding Time Trigger node does not exist");
        }
        Node node = nodeOp.get();
        node.nextsDeserialize();
        return node;
    }

    /**
     * helper method to create a dummy CoreModuleTask according to
     * the given dummy time node
     * @param timeTask given dummy task
     * @return a dummy CoreModuleTask
     * @throws DataBaseObjectNotFoundException if not found by given time
     */
    private CoreModuleTask createDummyCMTFromDummyTimeNode(TimeTask timeTask) throws DataBaseObjectNotFoundException {
        // retrieves node from repository
        Optional<Node> optionalNode = nodeRepository.findById(timeTask.getNodeId());
        Node curNode = initializeNodeFromDB(optionalNode);
        CoreModuleTask coreModuleTask = new CoreModuleTask();
        coreModuleTask.setName("dummyTimeTask");
        coreModuleTask.setCallapi(0);
        coreModuleTask.setType("dummy");
        coreModuleTask.setJourneyId(timeTask.getJourneyId());
        coreModuleTask.setNodeId(curNode.getId());
        coreModuleTask.setActiveAudienceId1(timeTask.activeAudienceId1SSerialize());
        coreModuleTask.setAudienceId1(timeTask.audienceId1SSerialize());
        coreModuleTask.setCreatedAt(LocalDateTime.now());
        coreModuleTask.setCreatedBy("TimeModule");
        coreModuleTask.setTaskType(0);
        coreModuleTask.setUpdatedAt(LocalDateTime.now());
        coreModuleTask.setUpdatedBy("TimeModule");
        coreModuleTask.setSourceNodeId(curNode.getId());
        return coreModuleTask;
    }
}