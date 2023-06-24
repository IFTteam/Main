package springredis.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.concurrent.ScheduledFuture;

/**
 * scheduler task thead pool config
 * can use @Scheduled for async doing a task
 */
@Configuration
public class SchedulerConfig {
    private final int POOL_SIZE = 8;
    private final ThreadPoolTaskScheduler scheduler;

    public SchedulerConfig() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(POOL_SIZE);
        scheduler.setThreadNamePrefix("my-scheduled-task-pool-");
        scheduler.initialize();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return scheduler;
    }
}
