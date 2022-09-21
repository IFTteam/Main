package springredis.demo.structures;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;
import springredis.demo.entity.Event;
import springredis.demo.entity.TimeTask;
import springredis.demo.repository.TimeDelayRepository;

import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

public class OutAPICaller implements Runnable{



    private TimeDelayRepository timeDelayRepository;
    private RedisTemplate redisTemplate;
    private RestTemplate restTemplate = new RestTemplate();
    private String outQueueKey = "OutQueue";
    public String timeKey = "triggerTime";
    public String idKey = "id";
    final String url = "http://localhost:3000";

    public OutAPICaller(TimeDelayRepository timeDelayRepository, RedisTemplate redisTemplate) {
        this.timeDelayRepository = timeDelayRepository;
        this.redisTemplate = redisTemplate;
    }

    //Debug??? connection with core module?
    @Override
    public void run() {
        while (redisTemplate.opsForList().size(outQueueKey)>0){
            HashMap outEvent = ((HashMap) redisTemplate.opsForList().rightPop(outQueueKey));
            Long id = ((Number)outEvent.get(idKey)).longValue();
            Optional<TimeTask> timeTaskOp = timeDelayRepository.findById(id);
            if (timeTaskOp.isPresent()){
                String result = restTemplate.postForObject(url, timeTaskOp.get(),String.class);
            }



        }

    }
}