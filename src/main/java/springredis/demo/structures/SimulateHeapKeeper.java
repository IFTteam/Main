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
                    // 因为从redis中取出的数据类型就是 springredis.demo.entity.Event ，所以不能强转换成HashMap类型的，
                    // 直接强制转换成 springredis.demo.entity.Event 类型就行了
                    Event outEvent = (Event) redisTemplate.opsForList().rightPop(inQueueKey);
                    if (outEvent != null) {
                        System.out.println("从redis中获取Event，ID="+outEvent.getId());
                        // 然后直接放到堆内存中。
                        MinHeap.heapInsert(outEvent);
                    }
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