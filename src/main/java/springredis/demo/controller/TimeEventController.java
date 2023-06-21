package springredis.demo.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
import springredis.demo.entity.TimeTask;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.TimeDelayRepository;

import java.nio.file.OpenOption;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    @ResponseBody
    @PostMapping("/addNewTask")
    public TimeTask add(@RequestBody BaseTaskEntity baseTaskEntity){
        Optional<Node> node = nodeRepository.findById(baseTaskEntity.getNodeId());
        Node node1 = node.get();
        // 为什么节点的名称，是fstring呢？
        String fstring = node1.getName();
        System.out.println("The fstring is " + fstring);
        CoreModuleTask coreModuleTask = new CoreModuleTask(baseTaskEntity);
        /*
        *   initialize new Time task
         */
        TimeTask timeTask = new TimeTask();
        timeTask.setCoreModuleTask(coreModuleTask);
        timeTask.setNodeId(baseTaskEntity.getNodeId());
        timeTask.activeAudienceId1SDeserialize(baseTaskEntity.getActiveAudienceId1());
        timeTask.activeAudienceId2SDeserialize(baseTaskEntity.getActiveAudienceId2());
        timeTask.audienceId1SDeserialize(baseTaskEntity.getAudienceId1());
        timeTask.audienceId2SDeserialize(baseTaskEntity.getAudienceId2());
        timeTask.setTaskStatus(0);
        
        parseFStringDelay(fstring, timeTask);
        
        System.out.println("======================================================TimeEventController Node ID: " + baseTaskEntity.getNodeId());
        System.out.println("======================================================TimeEventController TimeEvent Node ID: " + timeTask.getCoreModuleTask().getNodeId());
        //auditing support
        timeTask.setCreatedAt(LocalDateTime.now());
        timeTask.setCreatedBy("TimeModule");


        return timeDelayRepository.save(timeTask);
    }

//    public static void main(String[] args) {
//        TimeTask timeTask = new TimeTask();
//        parseFStringDelay("SpecificTime 2023-10-10 10:12:12 1 1", timeTask);
//        System.out.println(timeTask.getTriggerTime() +"    "+ timeTask.getRepeatTimes()+"    "+ timeTask.getRepeatInterval());
//
//        parseFStringDelay("DelayTimeInSecond 20 1 1", timeTask);
//        System.out.println(timeTask.getTriggerTime() +"    "+ timeTask.getRepeatTimes()+"    "+ timeTask.getRepeatInterval());
//
//        parseFStringDelay("TimeUnit 1 DAYS 1 1", timeTask);
//        System.out.println(timeTask.getTriggerTime() +"    "+ timeTask.getRepeatTimes()+"    "+ timeTask.getRepeatInterval());
//
//
//    }
    
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
    @PostMapping("/Time_Trigger")
    public CoreModuleTask Time_Trigger(@RequestBody CoreModuleTask coreModuleTask){
        Long node_id = coreModuleTask.getNodeId();
        System.out.println("(TimeEventController) CMT passed into Time_Trigger: " + coreModuleTask);
        System.out.println("node id: " + node_id);
        Node node = nodeRepository.findById(node_id).get();

        /*
        *   Set the dummy coreModuleTask
         */
        coreModuleTask.setMakenext(0);

        /*
        *   Initialize the new time task
         */
        TimeTask timeTask = new TimeTask(coreModuleTask);

        // set make next
        timeTask.setMakenext(coreModuleTask.getMakenext());

        /*
         *   Set the dummy coreModuleTask
         */
        coreModuleTask.setMakenext(0);

        timeTask.setTaskStatus(0);

        //parsing the time information
        JSONObject jsonObject = new JSONObject(node.getProperties());
        String time = jsonObject.getString("send");
        String frequency = jsonObject.getString("frequency");
        if (Objects.equals(frequency, "Once")) {
            time_parser_once(time, timeTask);
        }
        else if (Objects.equals(frequency, "Recurring")){
            time_parser_recurring(time, timeTask);
        }

        //auditing support
        timeTask.setNodeId(node_id);
        timeTask.setJourneyId(coreModuleTask.getJourneyId());
        timeTask.setCreatedAt(LocalDateTime.now());
        timeTask.setUserId(coreModuleTask.getUserId());
        timeTask.setCreatedBy(String.valueOf(coreModuleTask.getUserId()));
        timeTask.activeAudienceId1SDeserialize(coreModuleTask.getActiveAudienceId1());
        timeTask.activeAudienceId2SDeserialize(coreModuleTask.getActiveAudienceId2());
        timeTask.audienceId1SDeserialize(coreModuleTask.getAudienceId1());
        timeTask.audienceId2SDeserialize(coreModuleTask.getAudienceId2());
        timeTask.setTaskStatus(0);
        timeTask.setCoreModuleTask(coreModuleTask);
        timeTask.setJourneyId(coreModuleTask.getJourneyId());

        System.out.println("journey id before time" + timeTask.getCoreModuleTask().getJourneyId());
        System.out.println("The ActiveAudienceId1 is" + coreModuleTask.getActiveAudienceId1());
        //timeTask.setCreatedBy(String.valueOf(coreModuleTask.getAudienceId()));
        timeDelayRepository.save(timeTask);

        System.out.println("dummy task returned");
        System.out.println(coreModuleTask);
        return coreModuleTask;
    }

    @PostMapping("/Time_Delay")
    public CoreModuleTask Time_Delay(@RequestBody CoreModuleTask coreModuleTask){
        Long node_id = coreModuleTask.getNodeId();
        System.out.println("(TimeEventController) CMT passed into Time_Delay: " + coreModuleTask);
        System.out.println("node id: " + node_id);
        Node node = nodeRepository.findById(node_id).get();

        // Set the dummy coreModuleTask
        coreModuleTask.setMakenext(0);

        // Initialize the new time task
        TimeTask timeTask = createTimeTask(coreModuleTask, node_id);
        timeTask.setCreatedBy("Time delay");

        //parsing the time information
        JSONObject jsonObject = new JSONObject(node.getProperties());
        JSONObject example1 = new JSONObject("{\n" +
                "        \"date\": \"2023-05-10\",\n" +
                "      }");;
        JSONObject example2 = new JSONObject("{\n" +
                "        \"date\": \"1 Hours\",\n" +
                "      }");;
        String time = jsonObject.getString("date");
        String[] parsed = time.split(" ");
        if (parsed.length == 1) {
            time_parser_wait_date(parsed[0], timeTask);
        }
        else {
            time_parser_wait_duration(parsed, timeTask);
        }

        timeDelayRepository.save(timeTask);
        System.out.println("Saved Time Delay as TimeTask into timeDelayRepo");

        System.out.println("dummy task returned: " + coreModuleTask);
        return coreModuleTask;
    }

    private TimeTask createTimeTask(@RequestBody CoreModuleTask coreModuleTask, Long node_id) {
        TimeTask timeTask = new TimeTask(coreModuleTask);
        timeTask.setNodeId(node_id);
        timeTask.setJourneyId(coreModuleTask.getJourneyId());
        timeTask.setCreatedAt(LocalDateTime.now());
        timeTask.setUserId(coreModuleTask.getUserId());
        timeTask.setCreatedBy(String.valueOf(coreModuleTask.getUserId()));
        timeTask.activeAudienceId1SDeserialize(coreModuleTask.getActiveAudienceId1());
        timeTask.activeAudienceId2SDeserialize(coreModuleTask.getActiveAudienceId2());
        timeTask.audienceId1SDeserialize(coreModuleTask.getAudienceId1());
        timeTask.audienceId2SDeserialize(coreModuleTask.getAudienceId2());
        timeTask.setCoreModuleTask(coreModuleTask);
        timeTask.setJourneyId(coreModuleTask.getJourneyId());
        timeTask.setTaskStatus(0);
        return timeTask;
    }


    @PostMapping("/TimetasktestRepeat")
    public CoreModuleTask TimetasktestRepeat(@RequestBody CoreModuleTask coreModuleTask){
        Optional<Node> node = nodeRepository.findById(coreModuleTask.getNodeId());
        String fstring = node.get().getName();
        String[] flist = fstring.split(" ");
        // fstring format "specificTime(yyyy-MM-dd HH:mm:ss) repeatTimes repeatInterval" 2022-12-18 20:10:12 3 4
        // repeatInterval format "y m d"
        int repeatTimes = Integer.parseInt(flist[2]);
        for (int i = 0; i < repeatTimes; i++) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date parse = format.parse(flist[0] + " " + flist[1]);

                TimeTask timeTask = new TimeTask(coreModuleTask);
                timeTask.setTaskStatus(0);
                timeTask.setTriggerTime(parse.getTime());
                timeTask.setRepeatTimes(Integer.parseInt(flist[2]));
                timeTask.setRepeatInterval(flist[3]);
                //auditing support
                timeTask.setCreatedAt(LocalDateTime.now());
                timeTask.setCreatedBy(String.valueOf(coreModuleTask.getUserId()));
                //timeTask.setCreatedBy(String.valueOf(coreModuleTask.getAudienceId()));
                timeDelayRepository.save(timeTask);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
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
            String[] flist = fstring.split("T");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date parse = format.parse(flist[0] + " " + flist[1]);
            timeTask.setTriggerTime(parse.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void parseFStringWithFrequency(String frequency, TimeTask timeTask) {
        Calendar now = Calendar.getInstance();
        int weekday = now.get(Calendar.DAY_OF_WEEK);
        int tar_date = Calendar.MONDAY;
        switch (frequency){
            case "Monday":
                tar_date = Calendar.MONDAY;
                break;
            case "Tuesday":
                tar_date = Calendar.TUESDAY;
                break;
            case "Wednesday":
                tar_date = Calendar.WEDNESDAY;
                break;
            case "Thursday":
                tar_date = Calendar.THURSDAY;
                break;
            case "Friday":
                tar_date = Calendar.FRIDAY;
                break;
            case "Saturday":
                tar_date = Calendar.SATURDAY;
                break;
            case "Sunday":
                tar_date = Calendar.SUNDAY;
                break;
        }
        if (weekday != tar_date)
        {
            // calculate how much to add
            // the 2 is the difference between Saturday and Monday
            int days = (Calendar.SATURDAY - weekday + 2) % 7;
            now.add(Calendar.DAY_OF_YEAR, days);
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);

        }
        Date date = now.getTime();
        //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
        System.out.println("The recurring monday is:" + format);

    }

    private void parseFStringWithWaitTime(String waitTime, TimeTask timeTask) {
    }

    private void time_parser_once(String time, TimeTask timeTask) {
        String[] list = time.split("T");
        String clock = list[1];
        int hour = Integer.parseInt(clock.substring(0, 2));
        String minute = clock.substring(2, 5);
        String AMPM = clock.substring(clock.length() - 2);

        if (AMPM.equals("PM") && hour != 12) hour += 12;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date parse = format.parse(list[0] + " " + hour + minute);
            System.out.println("parsed time: " + parse);
            timeTask.setTriggerTime(parse.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void time_parser_recurring(String frequency, TimeTask timeTask) {
        String[] list = frequency.split(",");
        System.out.println(list.length);
        String d = list[0];
        String week = d.substring(0, d.length() - 4);
        String time = d.substring(d.length() - 4);
        String end_date = list[1].split(":")[0];
        Calendar now = Calendar.getInstance();
        int weekday = now.get(Calendar.DAY_OF_WEEK);
        int tar_date = Calendar.MONDAY;
        switch (week){
            case "Monday":
                tar_date = Calendar.MONDAY;
                break;
            case "Tuesday":
                tar_date = Calendar.TUESDAY;
                break;
            case "Wednesday":
                tar_date = Calendar.WEDNESDAY;
                break;
            case "Thursday":
                tar_date = Calendar.THURSDAY;
                break;
            case "Friday":
                tar_date = Calendar.FRIDAY;
                break;
            case "Saturday":
                tar_date = Calendar.SATURDAY;
                break;
            case "Sunday":
                tar_date = Calendar.SUNDAY;
                break;
        }
        if (weekday != tar_date)
        {
            int days = (Calendar.SATURDAY - weekday + 2) % 7;
            now.add(Calendar.DAY_OF_YEAR, days);
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
        }
        Date date = now.getTime();
        //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
        System.out.println("The recurring monday is:" + format);

    }

    private void time_parser_wait_date(String time, TimeTask timeTask){
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date parse = format.parse(time + " " +"00:00");
            timeTask.setTriggerTime(parse.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void time_parser_wait_duration(String[] time, TimeTask timeTask){
        int t = Integer.parseInt(time[0]);
        String unit = time[1];
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime date = null;
        switch (unit){
            case "Hours":
                date = now.plusHours(t);
                break;
            case "Days":
                date = now.plusDays(t);
                break;
            case "Weeks":
                date = now.plusWeeks(t);
                break;
            case "Months":
                date = now.plusMonths(t);
                break;
        }
        Date d = Date.from(Timestamp.valueOf(date).toInstant());
        timeTask.setTriggerTime(d.getTime());
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