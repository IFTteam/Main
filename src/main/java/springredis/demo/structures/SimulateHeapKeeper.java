package springredis.demo.structures;

import org.springframework.data.redis.core.RedisTemplate;
import springredis.demo.entity.Event;

import java.util.Date;
import java.util.HashMap;

public class SimulateHeapKeeper implements Runnable{


    private RedisTemplate redisTemplate;

    private String outQueueKey = "OutQueue";
    private String inQueueKey = "InQueue";
    public String timeKey = "triggerTime";
    public String idKey = "id";

    public SimulateHeapKeeper(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    @Override
    public void run() {

        while (true){
            synchronized (redisTemplate){
                if (!MinHeap.isEmpty()){
                    System.out.println( "Heap top is: "+MinHeap.getTopTime());
                    System.out.println("Time Now is: "+new Date());

                }

                while (!MinHeap.isEmpty() && MinHeap.getTopTime().getTime()< new Date().getTime()){
                    Event event = MinHeap.heapPop();
                    redisTemplate.opsForList().leftPush(outQueueKey,event);
                    System.out.println("Out Event: "+event.getTriggerTime());
                    System.out.println("OutQueue Size: "+redisTemplate.opsForList().size(outQueueKey));
                }

                while (redisTemplate.opsForList().size(inQueueKey)>0){
                    //maybe set a max loops
                    Event outEvent = (Event) redisTemplate.opsForList().rightPop(inQueueKey);
                    Date time = outEvent.getTriggerTime();
                    Long id = outEvent.getId();
                    Event event = new Event(time,id);
                    MinHeap.heapInsert(event);
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}