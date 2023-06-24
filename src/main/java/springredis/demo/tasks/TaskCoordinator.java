package springredis.demo.tasks;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import springredis.demo.entity.CoreModuleTask;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
public class TaskCoordinator {
    private final RedisTemplate m_redisTemplate;

    private final CMTExecutor cmtExecutor;

    private final TaskScheduler taskScheduler;

    private ScheduledFuture<?> scheduledFuture;

    @Value("${redis-key.task-queue-key}")
    private String taskQueueKey;

    /**
     * corn Expression to determine current running status
     */
    @Setter
    @Getter
    @Value("${corn.expressions.everySecond}")
    private String cronExpression;

    // dao 和service注入
    @Autowired
    public TaskCoordinator(RedisTemplate redisTemplate, CMTExecutor cmtExecutor, TaskScheduler taskScheduler) {
        this.m_redisTemplate = redisTemplate;
        this.cmtExecutor = cmtExecutor;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        log.info("start TaskCoordinator...");
        RedisConnection redisConnection = RedisConnectionUtils.getConnection(Objects.requireNonNull(m_redisTemplate.getConnectionFactory()), true);
        redisConnection.flushDb();
        startTaskCoordinator(cronExpression);
    }

    /**
     * start processTaskCoordinator() with given cronExpression
     * @param cronExpression given cronExpression
     */
    public void startTaskCoordinator(String cronExpression) {
        log.info("start running TaskCoordinator for {} ...", cronExpression);
        Runnable task = this::processTaskCoordinator;
        setCronExpression(cronExpression);
        scheduledFuture = taskScheduler.schedule(task, new CronTrigger(cronExpression));
    }

    /**
     * stop processTaskCoordinator()
     */
    @PreDestroy
    public void stopTaskCoordinator() {
        log.info("stop running TaskCoordinator...");
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(true);
        }
    }

    private void processTaskCoordinator() {
        // 查Redis
        while (m_redisTemplate.opsForList().size(taskQueueKey) > 0) {
            log.info("redis indicate more task to be run...");
            CoreModuleTask coreModuleTask = (CoreModuleTask) m_redisTemplate.opsForList().rightPop(taskQueueKey);
            log.info(coreModuleTask.toString());
            cmtExecutor.execute(coreModuleTask);
        }
    }
}
