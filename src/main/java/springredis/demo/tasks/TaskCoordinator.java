package springredis.demo.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import springredis.demo.repository.TimeDelayRepository;
import springredis.demo.structures.OutAPICaller;
import springredis.demo.structures.SimulateHeapKeeper;
import springredis.demo.structures.SimulateNewEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class TaskCoordinator implements DisposableBean {

    private volatile boolean someCondition = true;

    private final String taskQueueKey = "CoretaskQueue";



    private ExecutorService executorService;

    // dao 和service注入
    @Autowired
    public TaskCoordinator(RedisTemplate redisTemplate, TimeDelayRepository timeDelayRepository) {
        RedisConnection redisConnection = RedisConnectionUtils.getConnection(redisTemplate.getConnectionFactory(),true);
        redisConnection.flushDb();
        SimulateHeapKeeper simulateHeapKeeper = new SimulateHeapKeeper(redisTemplate);
        OutAPICaller outAPICaller = new OutAPICaller(timeDelayRepository, redisTemplate);
        SimulateNewEvent simulateNewEvent = new SimulateNewEvent(timeDelayRepository, redisTemplate);
        new Thread(simulateNewEvent).start();
        new Thread(simulateHeapKeeper).start();
        new Thread(outAPICaller).start();
    }



    @Override
    public void destroy() throws Exception {
        someCondition = false;
    }
}
