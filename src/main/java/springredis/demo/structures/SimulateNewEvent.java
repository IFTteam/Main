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
import org.springframework.web.client.RestTemplate;
import springredis.demo.entity.Event;
import springredis.demo.entity.TimeTask;
import springredis.demo.repository.TimeDelayRepository;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class SimulateNewEvent {


    private final TimeDelayRepository timeDelayRepository;
    private final RedisTemplate redisTemplate;
    private RestTemplate restTemplate = new RestTemplate();

    private ScheduledFuture<?> scheduledFuture;

    /**
     * auto-wired taskScheduler to do scheduling task
     */
    private final TaskScheduler taskScheduler;

    /**
     * cron Expression to determine current running status
     */
    @Setter
    @Getter
    @Value("${corn.expressions.everyTenSecond}")
    private String cronExpression;

    @Value("${redis-key.in-queue-key}")
    private String inQueueKey;


    @Autowired
    public SimulateNewEvent(TimeDelayRepository timeDelayRepository, RedisTemplate redisTemplate, TaskScheduler taskScheduler) {
        this.redisTemplate = redisTemplate;
        this.timeDelayRepository = timeDelayRepository;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        log.info("start SimulateNewEvent...");
        startSimulateNewEvent(cronExpression);
    }

    /**
     * start processSimulateNewEvent() with given cronExpression
     * @param cronExpression given cronExpression
     */
    public void startSimulateNewEvent(String cronExpression) {
        log.info("start running SimulateNewEvent for {} ...", cronExpression);
        Runnable task = this::processSimulateNewEvent;
        setCronExpression(cronExpression);
        scheduledFuture = taskScheduler.schedule(task, new CronTrigger(cronExpression));
    }

    /**
     * stop processSimulateNewEvent()
     */
    @PreDestroy
    public void stopSimulateNewEvent() {
        log.info("stop running SimulateNewEvent...");
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(true);
        }
    }

    public void processSimulateNewEvent() {
        int timeAhead = 0;//the time before the task trigger that we bring a task from sql to redis(ms)
        Date time = new Date();
        System.out.println(time.getTime());
        List<TimeTask> timeTasks = timeDelayRepository.findTasksBeforeTime(time.getTime() + timeAhead);
        for (TimeTask timeTask : timeTasks) {
            log.info(Thread.currentThread().getName() + "caught a triggered time task...");
            time.setTime(timeTask.getTriggerTime());
            Event event = new Event(time, timeTask.getId());
            timeTask.setTaskStatus(1);
            timeDelayRepository.save(timeTask);

            redisTemplate.opsForList().leftPush(inQueueKey, event);
            System.out.println("Insert New Event at" + time);
        }
    }
}