package springredis.demo.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;
import springredis.demo.repository.TimeDelayRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;
import springredis.demo.structures.OutAPICaller;
import springredis.demo.structures.SimulateHeapKeeper;
import springredis.demo.structures.SimulateNewEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class TaskCoordinator implements DisposableBean,Runnable {

    private volatile boolean someCondition = true;

    private final String taskQueueKey = "CoretaskQueue";

    private RedisTemplate m_redisTemplate;

    @Autowired
    CMTExecutor cmtExecutor;


    private ExecutorService executorService;

    // dao 和service注入
    @Autowired
    public TaskCoordinator(RedisTemplate redisTemplate, TimeDelayRepository timeDelayRepository, NodeRepository nodeRepository, ActiveNodeRepository activeNodeRepository) {
        RedisConnection redisConnection = RedisConnectionUtils.getConnection(redisTemplate.getConnectionFactory(),true);
        redisConnection.flushDb();
        SimulateHeapKeeper simulateHeapKeeper = new SimulateHeapKeeper(redisTemplate);
        OutAPICaller outAPICaller = new OutAPICaller(timeDelayRepository, redisTemplate, nodeRepository, activeNodeRepository);
        SimulateNewEvent simulateNewEvent = new SimulateNewEvent(timeDelayRepository, redisTemplate);
        this.m_redisTemplate = redisTemplate;
        new Thread(simulateNewEvent).start();
        new Thread(simulateHeapKeeper).start();
        new Thread(outAPICaller).start();
        new Thread(this).start();                   //since task coordinator itself is also a runnable,its run function is also started when it is constructed
    }


    @Override
    public void run() {
//        ExecutorService executorService = Executors.newFixedThreadPool(10);//初始化线程池
        while (someCondition) {
            // 查Redis
            while (m_redisTemplate.opsForList().size(taskQueueKey)>0){
                System.out.println("========== (TaskCoordinator) New CMT detected in CoreTaskQueue, start executing ========");
                CoreModuleTask coreModuleTask = (CoreModuleTask) m_redisTemplate.opsForList().rightPop(taskQueueKey);
                cmtExecutor.execute(coreModuleTask);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        someCondition = false;
    }
}
