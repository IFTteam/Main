package springredis.demo.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import springredis.demo.entity.CoreModuleTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class TaskCoordinator implements DisposableBean,Runnable {

    private Thread thread;
    private volatile boolean someCondition = true;

    private final String taskQueueKey = "CoretaskQueue";
    @Autowired
    private RedisTemplate redisTemplate;
    private ExecutorService executorService;

    // dao 和service注入
    @Autowired
    public TaskCoordinator() {
            this.thread = new Thread(this);
            this.thread.start();
            log.info("Task Coordinator Started");
        }

        @Override
        public void run() {
            ExecutorService executorService = Executors.newFixedThreadPool(10);//初始化线程池


        while (someCondition) {
            // 查Redis
            while (redisTemplate.opsForList().size(taskQueueKey)>0){
                CoreModuleTask coreModuleTask = (CoreModuleTask) redisTemplate.opsForList().rightPop(taskQueueKey);
                TaskExecutor taskExecutor = new TaskExecutor(coreModuleTask);
                executorService.execute(taskExecutor);
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

