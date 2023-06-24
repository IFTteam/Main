package springredis.demo.structures;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import springredis.demo.entity.Event;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
public class SimulateHeapKeeper {

    private final RedisTemplate redisTemplate;

    /**
     * auto-wired taskScheduler to do scheduling task
     */
    private final TaskScheduler taskScheduler;

    /**
     * corn Expression to determine current running status
     */
    @Setter
    @Getter
    @Value("${corn.expressions.everySecond}")
    private String cronExpression;

    @Value("${redis-key.out-queue-key}")
    private String outQueueKey;

    @Value("${redis-key.in-queue-key}")
    private String inQueueKey;

    private ScheduledFuture<?> scheduledFuture;


    @Autowired
    public SimulateHeapKeeper(RedisTemplate redisTemplate, TaskScheduler taskScheduler) {
        this.redisTemplate = redisTemplate;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        log.info("start SimulateHeapKeeper...");
        startSimulateHeapKeeper(cronExpression);
    }

    /**
     * start processSimulateHeapKeeper() with given cronExpression
     * @param cronExpression given cronExpression
     */
    public void startSimulateHeapKeeper(String cronExpression) {
        log.info("start running SimulateHeapKeeper for {} ...", cronExpression);
        Runnable task = this::processSimulateHeapKeeper;
        setCronExpression(cronExpression);
        scheduledFuture = taskScheduler.schedule(task, new CronTrigger(cronExpression));
    }

    /**
     * stop processSimulateHeapKeeper()
     */
    @PreDestroy
    public void stopSimulateHeapKeeper() {
        log.info("stop running SimulateHeapKeeper...");
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(true);
        }
    }

    /**
     * This will be executed every 1 second(by default) according to cornExpression
     * this method will pop inQueueKey object into MinHeap first,
     * if MinHeap's top object.triggerTime < your local time, then
     * pop from MinHeap's top and push into outQueueKey from redis
     */
    private void processSimulateHeapKeeper(){
        synchronized (redisTemplate) {
            if (!MinHeap.isEmpty()) {
                System.out.println("Heap top is: " + MinHeap.getTopTime());
                System.out.println("Time Now is: " + new Date());
            }

            while (!MinHeap.isEmpty() && MinHeap.getTopTime().getTime() < System.currentTimeMillis()) {
                Event event = MinHeap.heapPop();
                System.out.println("========== (SimulateHeapKeeper) Popped Event from Heap Top and Pushed into outQueue ==========");
                redisTemplate.opsForList().leftPush(outQueueKey, event);
                System.out.println("Out Event: " + event.getTriggerTime());
                System.out.println("OutQueue Size: " + redisTemplate.opsForList().size(outQueueKey));
            }

            while (redisTemplate.opsForList().size(inQueueKey) > 0) {
                log.info("(SimulateHeapKeeper) Detected Event from inQueue and Insert into Heap");
                // maybe set a max loops
                System.out.println("========== (SimulateHeapKeeper) Detected Event from inQueue and Insert into Heap ==========");
                Event outEvent = (Event) redisTemplate.opsForList().rightPop(inQueueKey);
                Date time = outEvent.getTriggerTime();
                Long id = outEvent.getId();
                Event event = new Event(time, id);
                MinHeap.heapInsert(event);
            }
        }
    }
}