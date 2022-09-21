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
                }

                while (redisTemplate.opsForList().size(inQueueKey)>0){
                    //maybe set a max loops
                    HashMap outEvent = ((HashMap) redisTemplate.opsForList().rightPop(inQueueKey));
                    Date time = new Date ((Integer) outEvent.get(timeKey));
                    Integer id = (Integer) outEvent.get(idKey);

                    Event event = new Event(time,(long)id);
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