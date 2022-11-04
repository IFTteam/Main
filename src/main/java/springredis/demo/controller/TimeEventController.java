package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import springredis.demo.entity.base.BaseEntity;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

import springredis.demo.entity.CoreModuleTask;

@RestController
public class TimeEventController {
    @GetMapping("/addNewTask")
    public TimeTask add(@RequestBody BaseTaskEntity baseTaskEntity){
        Optional<Node> node = nodeRepository.findById(baseTaskEntity.getNodeId());
        Node node1 = node.get();
        // 为什么节点的名称，是fstring呢？
        String fstring = node1.getName();
        TimeTask timeTask = new TimeTask(baseTaskEntity);
        timeTask.setTaskStatus(0);
        parseFStringDelay(fstring, timeTask);

        //auditing support
        timeTask.setCreatedAt(LocalDateTime.now());
        timeTask.setCreatedBy("TimeModule");

        List<Long> activeAudienceId1 = timeTask.getActiveAudienceId1();
        activeAudienceId1.add(234L);
        return timeDelayRepository.save(timeTask);
    }
    public static void main(String[] args) {
        TimeTask timeTask = new TimeTask();
        parseFStringDelay("SpecificTime 2023-10-10 10:12:12 1 1", timeTask);
        System.out.println(timeTask.getTriggerTime() +"    "+ timeTask.getRepeatTimes()+"    "+ timeTask.getRepeatInterval());

        parseFStringDelay("DelayTimeInSecond 20 1 1", timeTask);
        System.out.println(timeTask.getTriggerTime() +"    "+ timeTask.getRepeatTimes()+"    "+ timeTask.getRepeatInterval());

        parseFStringDelay("TimeUnit 1 DAYS 1 1", timeTask);
        System.out.println(timeTask.getTriggerTime() +"    "+ timeTask.getRepeatTimes()+"    "+ timeTask.getRepeatInterval());


    }
    public static void parseFStringDelay(String fstring,TimeTask timeTask){

//        SpecificTime 2023-10-10 10:12:12 1
        String type = fstring.split(" ")[0];
        if ("DelayTimeInSecond".equals(type)) {
            parseFStringDelayTimeInSecond(fstring.replace("DelayTimeInSecond ", ""), timeTask);
        }else  if ("SpecificTime".equals(type)) {
            parseFStringWithSpecificTime(fstring.replace("SpecificTime ", ""), timeTask);
        }else  if ("TimeUnit".equals(type)) {
            parseFStringWithTimeUnit(fstring.replace("TimeUnit ", ""), timeTask);
        }
    }
    @Autowired
    private TimeDelayRepository timeDelayRepository;
    @Autowired
    private NodeRepository nodeRepository;

    @GetMapping("/allTasks")
    public List<TimeTask> getAll(){
        return timeDelayRepository.findAll();
    }

    @PostMapping("/addNewTaskWithSpecificTime")
    public TimeTask addNewTaskWithSpecificTime(@RequestBody BaseTaskEntity baseTaskEntity){
        Optional<Node> node = nodeRepository.findById(baseTaskEntity.getNodeId());
        Node node1 = node.get();
        String fstring = node1.getName();
        TimeTask timeTask = new TimeTask(baseTaskEntity);
        timeTask.setTaskStatus(0);
        parseFStringWithSpecificTime(fstring, timeTask);

        //auditing support
        timeTask.setCreatedAt(LocalDateTime.now());
        timeTask.setCreatedBy("TimeModule");
        return timeDelayRepository.save(timeTask);
    }


    /**
     *
     * Regarding the add method in TimeEventController
     *
     * Change parameter to CoreModuleTask
     *
     * Change return object to CoreModuleTask
     *
     * For the input CoreModuleTask
     *
     * Change status to 0
     *
     * Return the same CoreModuleTask that is inputted
     *
     * Still save the TimeTask to TimeDelayRepository
     */
    @PostMapping("/add")
    public CoreModuleTask add(@RequestBody CoreModuleTask coreModuleTask){
        Optional<Node> node = nodeRepository.findById(coreModuleTask.getNodeId());
        TimeTask timeTask = new TimeTask(coreModuleTask);
        timeTask.setTaskStatus(0);
        parseFStringWithSpecificTime(node.get().getName(), timeTask);

        //auditing support
        timeTask.setCreatedAt(LocalDateTime.now());
        timeTask.setCreatedBy(String.valueOf(coreModuleTask.getUserId()));
        //timeTask.setCreatedBy(String.valueOf(coreModuleTask.getAudienceId()));
        timeDelayRepository.save(timeTask);
        return coreModuleTask;
    }





    // need modification for set time trigger
    private static void parseFStringDelayTimeInSecond(String fstring, TimeTask timeTask) {
        // 数据格式： fstring format "DelayTimeInSecond repeatTimes repeatInterval"
        // repeatInterval format "y m d"
        String[] flist = fstring.split(" ");
        Date date = new Date();
        // ms
        timeTask.setTriggerTime(date.getTime()+Long.parseLong(flist[0])*1000);//Trigger Time = Time now + Delay Time
        timeTask.setRepeatTimes(Integer.parseInt(flist[1]));
        timeTask.setRepeatInterval(flist[2]);
    }

    // need modification for set time trigger
    private static void parseFStringWithSpecificTime(String fstring, TimeTask timeTask) {
        try {
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

    private static void parseFStringWithTimeUnit(String fstring, TimeTask timeTask) {
        try {
            // fstring format "num timeUnit(DAYS,HOURS,WEEKS) repeatTimes repeatInterval"
            // repeatInterval format "y m d"
            // eg:  1 DAYS 1 2
            String[] flist = fstring.split(" ");
            int num = Integer.parseInt(flist[0]);
            String timeUnit = flist[1];
            if (TimeUnit.DAYS.name().equals(timeUnit)) {
                timeTask.setTriggerTime(new Date().getTime() + TimeUnit.DAYS.toMillis(num));
            }else if (TimeUnit.HOURS.name().equals(timeUnit)) {
                timeTask.setTriggerTime(new Date().getTime() + TimeUnit.HOURS.toMillis(num));
            }else if ("WEEKS".equals(timeUnit)) {
                timeTask.setTriggerTime(new Date().getTime() + TimeUnit.DAYS.toMillis(7*num));
            }
            timeTask.setRepeatTimes(Integer.parseInt(flist[2]));
            timeTask.setRepeatInterval(flist[3]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @GetMapping ("/taskbytime")

    public List<TimeTask> getTaskByTime(@PathVariable(value = "triggerTime", required = false)Long triggerTime){

        return timeDelayRepository.findTasksBeforeTime(triggerTime);
    }

   /* @GetMapping("/TimeDelay")
    public List<TimeTask> TimeDelay(CoreModuleTask coreModuleTask){
        System.out.println();
        Long id = coreModuleTask.getId();
        Long nodeid = coreModuleTask.getNodeId();

        return null;
    }
    */


}