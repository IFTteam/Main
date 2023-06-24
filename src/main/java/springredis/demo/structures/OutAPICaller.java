package springredis.demo.structures;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

    //    private boolean isRunning = true;
    private String outQueueKey = "OutQueue";
    public String timeKey = "triggerTime";
    public String idKey = "id";
    final String url = "http://localhost:3000";
    private final HashMap<String, String> urlDict = new HashMap<>() {{
        put("Time Delay", "http://localhost:8080/Time_Delay");
        put("API Trigger", "http://localhost:8080/API_trigger");
        put("Time Trigger", "http://localhost:8080/Time_Trigger");
        put("Send Email", "http://localhost:8080/actionSend/createCMTTransmission");
        put("If/Else", "http://localhost:8080/IfElse");
        put("Add Tag", "http://localhost:8080/AddTag");
    }};

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
            if (timeTaskOp.isEmpty()) {
                throw new DataBaseObjectNotFoundException("No Time Task Exist");
            }
            TimeTask timetask = timeTaskOp.get();
            timetask.audience_serialize();
            System.out.println("================================================Time Task retrieved=====================================================");
            System.out.println("cur JI id is:" + timetask.getJourneyId());
            System.out.println("Time Task Node: " + timetask);
            Optional<Node> optionalNode = nodeRepository.findById(timetask.getNodeId());  //retrieves node from repository
            if (optionalNode.isEmpty()) {
                throw new DataBaseObjectNotFoundException("The corresponding Time Trigger node does not exist");
            }
            Node node = initializeNodeFromDB(optionalNode);

            System.out.println("================================================Time Trigger node retrieved=====================================================");

            //for now, assume we only have one branch in the journey, so we only take nexts[0]
            System.out.println("Node getNexts Index 0: " + node.getNexts().get(0));
            Long next_node_id = node.getNexts().get(0);
            Optional<Node> optionalNextNode = nodeRepository.findById(next_node_id);  //find next node by id from repository
            if (optionalNextNode.isEmpty()) {
                throw new DataBaseObjectNotFoundException("The Next node does not exist");
            }
            if (node.getNexts().size() > 1) {
                // TODO: if more than one branch in the journey exists, there could be multiple next nodes
            }
            Node nextNode = initializeNodeFromDB(optionalNextNode);

            //CoreModuleTask nextCoreModuleTask = new CoreModuleTask(coreModuleTask);  //create new CoreModuleTask based on current CoreModuleTask
            CoreModuleTask nextCoreModuleTask = new CoreModuleTask();
            //System.out.println("cur CM id is:" + coreModuleTask);

            nextCoreModuleTask.setType(nextNode.getType());
            nextCoreModuleTask.setName(nextNode.getName());
            //This information will be lost when saved into DB. Does CoreModuleTask need its own attributes for nodeId and audience?
            System.out.println("next node id is:" + nextNode.getId());
            nextCoreModuleTask.setJourneyId(timetask.getJourneyId());
            nextCoreModuleTask.setNodeId(nextNode.getId());  //set the node id to next node
//            	nextCoreModuleTask.setActiveAudienceId1(timeTaskOp.get().activeAudienceId1SSerialize());
//            	nextCoreModuleTask.setActiveAudienceId2(timeTaskOp.get().activeAudienceId2SSerialize());
            System.out.println("TTAA1 info" + timetask.getActiveAudienceId1());
            System.out.println("TTAA2 info" + timetask.getActiveAudienceId2());
            System.out.println("TTA1 info" + timetask.getAudienceId1());
            System.out.println("TTA2 info" + timetask.getAudienceId2());
            nextCoreModuleTask.setActiveAudienceId1(timetask.activeAudienceId1SSerialize());
            nextCoreModuleTask.setActiveAudienceId2(timetask.audienceId2SSerialize());
            nextCoreModuleTask.setAudienceId1(timetask.audienceId1SSerialize());
            nextCoreModuleTask.setAudienceId2(timetask.audienceId2SSerialize());
            System.out.println("NAA1 info" + nextCoreModuleTask.getActiveAudienceId1());
            System.out.println("NAA2 info" + nextCoreModuleTask.getActiveAudienceId2());
            System.out.println("NA1 info" + nextCoreModuleTask.getAudienceId1());
            System.out.println("NA2 info" + nextCoreModuleTask.getAudienceId2());
            //auditing support
            nextCoreModuleTask.setCreatedAt(LocalDateTime.now());
            nextCoreModuleTask.setCreatedBy("TimeModule");

            System.out.println("================================================OUTAPI successful 3=====================================================");

            String type = nextCoreModuleTask.getName();
            System.out.println("In outAPI the CM is:" + nextCoreModuleTask);
            System.out.println("In outAPI the type is:" + type);
            String url = urlDict.get(type);
            System.out.println("the url is " + url);

//				String result = restTemplate.postForObject(url, nextCoreModuleTask, String.class);
            cmtExecutor.execute(nextCoreModuleTask);
        }
    }

    public Node initializeNodeFromDB(Optional<Node> NodeOp) {
        Node node = NodeOp.get();
        node.nextsDeserialize();
        node.setLasts(new ArrayList<>());
        return node;
    }

}