package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
//the API controller to receive task from other module
@RestController
public class TaskController {
    @Autowired
    RedisTemplate redisTemplate;

    private final String taskQueueKey = "CoretaskQueue";
    @PostMapping("/ReturnTask")
    public Long addTask(@RequestBody CoreModuleTask coreModuleTask){

        return redisTemplate.opsForList().leftPush(taskQueueKey ,coreModuleTask);
    }

}
