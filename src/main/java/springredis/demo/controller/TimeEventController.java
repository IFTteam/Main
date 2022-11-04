package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.entity.Node;
import springredis.demo.entity.TimeTask;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.TimeDelayRepository;

import java.nio.file.OpenOption;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    @PostMapping("/add")
    public CoreModuleTask add(@RequestBody CoreModuleTask coreModuleTask){
        Optional<Node> node = nodeRepository.findById(coreModuleTask.getNodeId());
        TimeTask timeTask = new TimeTask(coreModuleTask);
        timeTask.setTaskStatus(0);
        parseFStringWithSpecificTime(node.get().getName(), timeTask);

        //auditing support
        timeTask.setCreatedAt(LocalDateTime.now());
        timeTask.setCreatedBy(String.valueOf(coreModuleTask.getUserId()));
        timeTask.setCreatedBy(String.valueOf(coreModuleTask.getAudienceId()));
        timeDelayRepository.save(timeTask);
        return coreModuleTask;
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

    // need modification for set time trigger
    private static void parseFStringWithSpecificTime(String fstring, TimeTask timeTask) {
        try {
            // 2022-10-20 10:00:00 1 1
            // fstring format "specificTime(yyyy-MM-dd HH:mm:ss) repeatTimes repeatInterval"
            // repeatInterval format "y m d"
            String[] flist = fstring.split(" ");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date parse = format.parse(flist[0] + " " + flist[1]);
            timeTask.setTriggerTime(parse.getTime());
            timeTask.setRepeatTimes(Integer.parseInt(flist[2]));
            timeTask.setRepeatInterval(flist[3]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        TimeTask timeTask = new TimeTask();
        parseFStringWithSpecificTime("2022-10-10 10:12:00 1 1", timeTask);
        System.out.println(timeTask);
        Date date = new Date(1665367920000L);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String format1 = format.format(date);
        System.out.println(format1);
    }

    @GetMapping("/taskbytime")
    public List<TimeTask> getTaskByTime(@RequestParam("triggerTime")Long triggerTime){
        return timeDelayRepository.findTasksBeforeTime(triggerTime);
    }

}