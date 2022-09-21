package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.entity.Node;
import springredis.demo.entity.TimeTask;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.TimeDelayRepository;

import java.nio.file.OpenOption;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
public class TimeEventController {
    @Autowired
    private TimeDelayRepository timeDelayRepository;
    @Autowired
    private NodeRepository nodeRepository;

    @GetMapping("/allTasks")
    public List<TimeTask> getAll(){
        return timeDelayRepository.findAll();
    }

    @PostMapping("/addNewTask")
    public TimeTask add(@RequestBody BaseTaskEntity baseTaskEntity){
        Optional<Node> node = nodeRepository.findById(baseTaskEntity.getNodeId());
        Node node1 = node.get();
        String fstring = node1.getName();
        TimeTask timeTask = new TimeTask(baseTaskEntity);
        timeTask.setTaskStatus(0);
        parseFString(fstring, timeTask);

        //auditing support
        timeTask.setCreatedAt(LocalDateTime.now());
        timeTask.setCreatedBy("TimeModule");


        return timeDelayRepository.save(timeTask);
    }


    // need modification for set time trigger
    private void parseFString(String fstring, TimeTask timeTask) {
        // fstring format "DelayTimeInSecond repeatTimes repeatInterval"
        // repeatInterval format "y m d"
        String[] flist = fstring.split(" ");
        Date date = new Date();
        timeTask.setTriggerTime(date.getTime()+Long.parseLong(flist[0])*1000);//Trigger Time = Time now + Delay Time
        timeTask.setRepeatTimes(Integer.parseInt(flist[1]));
        timeTask.setRepeatInterval(flist[2]);
    }

    @GetMapping("/taskbytime")
    public List<TimeTask> getTaskByTime(@RequestParam("triggerTime")Long triggerTime){
        return timeDelayRepository.findTasksBeforeTime(triggerTime);
    }

}