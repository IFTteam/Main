package springredis.demo.structures;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;
import springredis.demo.entity.Event;
import springredis.demo.entity.TimeTask;
import springredis.demo.repository.TimeDelayRepository;

import java.util.Date;
import java.util.List;


public class SimulateNewEvent implements Runnable{


    private TimeDelayRepository timeDelayRepository;
    private RedisTemplate redisTemplate;
    private RestTemplate restTemplate = new RestTemplate();

    public SimulateNewEvent(TimeDelayRepository timeDelayRepository, RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.timeDelayRepository = timeDelayRepository;
    }


    private String inQueueKey = "InQueue";



    @Override
    public void run() {
        int timeAhead = 0;//the time before the task trigger that we bring a task from sql to redis(ms)
        int scanInterval = 10000;
        while(true){
            Date time = new Date();
            System.out.println(time.getTime());

            System.out.println(time.getTime());
            List<TimeTask> timeTasks = timeDelayRepository.findTasksBeforeTime(time.getTime()+timeAhead);
            for (TimeTask timeTask : timeTasks){
                System.out.println("========== (SimulateNewEvent) Found TimeTask before NOW ==========");
                time.setTime(timeTask.getTriggerTime());
                Event event = new Event(time,timeTask.getId());
                timeTask.setTaskStatus(1);
                timeDelayRepository.save(timeTask);

                redisTemplate.opsForList().leftPush(inQueueKey,event);
                System.out.println("========== (SimulateNewEvent) Pushed into inQueue, New Event at " + time + " ==========");

                //TODO: return response to Core Module with taskEntity
                System.out.println("========== (SimulateNewEvent) Calling ReturnTask API with timeTask ==========");
                restTemplate.postForObject("http://localhost:8080/ReturnTask", timeTask, String.class);
            }




            try {
                Thread.sleep(scanInterval);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

//    public int getRandom(int min, int max) {
//        return (int) min + (int) (Math.random() * (max - min));
//    }
}