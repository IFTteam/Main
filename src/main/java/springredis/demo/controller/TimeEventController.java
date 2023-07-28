package springredis.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
import springredis.demo.entity.TimeTask;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.TimeDelayRepository;
import springredis.demo.utils.OptionalUtils;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class TimeEventController {
    @Autowired
    private TimeDelayRepository timeDelayRepository;
    @Autowired
    private NodeRepository nodeRepository;

    @GetMapping("/allTasks")
    public List<TimeTask> getAll() {
        return timeDelayRepository.findAll();
    }

    @ResponseBody
    @PostMapping("/addNewTask")
    public TimeTask add(@RequestBody BaseTaskEntity baseTaskEntity) {
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

    public static void parseFStringDelay(String fstring, TimeTask timeTask) {
//        SpecificTime 2023-10-10 10:12:12 1
        String type = fstring.split(" ")[0];
        if ("DelayTimeInSecond".equals(type)) {
            parseFStringDelayTimeInSecond(fstring.replace("DelayTimeInSecond ", ""), timeTask);
        } else if ("SpecificTime".equals(type)) {
            parseFStringWithSpecificTime(fstring.replace("SpecificTime ", ""), timeTask);
        } else if ("TimeUnit".equals(type)) {
            parseFStringWithTimeUnit(fstring.replace("TimeUnit ", ""), timeTask);
        }
    }

    @PostMapping("/addNewTaskWithSpecificTime")
    public TimeTask addNewTaskWithSpecificTime(@RequestBody BaseTaskEntity baseTaskEntity) {
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
     * Regarding the add method in TimeEventController
     * <p>
     * Change parameter to CoreModuleTask
     * <p>
     * Change return object to CoreModuleTask
     * <p>
     * For the input CoreModuleTask
     * <p>
     * Change status to 0
     * <p>
     * Return the same CoreModuleTask that is inputted
     * <p>
     * Still save the TimeTask to TimeDelayRepository
     */
    @PostMapping("/Time_Trigger")
    public CoreModuleTask saveTimeTaskByTimeTrigger(@RequestBody CoreModuleTask coreModuleTask) {
        Long nodeId = coreModuleTask.getNodeId();
        log.info("begin to save new time task(s) from Time_Trigger path with node id: {}...", nodeId);

        Optional<Node> optionalNode = nodeRepository.findById(nodeId);
        Node node = OptionalUtils.getObjectOrThrow(optionalNode, "Not found node by given node id");

//        TimeTask timeTask = new TimeTask(coreModuleTask);

        //Set the dummy coreModuleTask
        coreModuleTask.setMakenext(0);

        //parsing the time information
        JSONObject jsonObject = new JSONObject(node.getProperties());
        String time = jsonObject.getString("send");
        String frequency = jsonObject.getString("frequency");
        String endDate = jsonObject.getString("end date");
        if ("Once".equals(frequency)) {
            timeParserOnce(time, coreModuleTask);
        } else if ("Recurring".equals(frequency)) {
            timeParserRecurring(time, coreModuleTask, endDate);
        }
        return coreModuleTask;
    }

    @PostMapping("/Time_Delay")
    public CoreModuleTask saveTimeTaskByTimeDelay(@RequestBody CoreModuleTask coreModuleTask) throws SQLException {
        log.info("begin to save a new time task from Time_Delay path...");
        Long nodeId = coreModuleTask.getNodeId();
        System.out.println("(TimeEventController) CMT passed into Time_Delay: " + coreModuleTask);
        System.out.println("node id: " + nodeId);
        Optional<Node> optionalNode = nodeRepository.findById(nodeId);

        if (optionalNode.isEmpty()) {
            log.error("Cannot find certain by node id in Time_Delay path");
            throw new SQLException("exception when running Time_Delay path");
        }

        Node node = optionalNode.get();


        // Initialize the new time task
        TimeTask timeTask = createTimeTask(coreModuleTask);
        timeTask.setCreatedBy("Time delay");
        timeTask.setMakenext(coreModuleTask.getMakenext());

        // Set the dummy coreModuleTask
        coreModuleTask.setMakenext(0);

        //parsing the time information
        JSONObject jsonObject = new JSONObject(node.getProperties());
        String time = jsonObject.getString("date");
        String[] parsed = time.split(" ");
        if (parsed.length == 1) {
            time_parser_wait_date(parsed[0], timeTask);
        } else {
            time_parser_wait_duration(parsed, timeTask);
        }

        timeDelayRepository.save(timeTask);
        System.out.println("Saved Time Delay as TimeTask into timeDelayRepo");

        System.out.println("dummy task returned: " + coreModuleTask);
        return coreModuleTask;
    }

    private TimeTask createTimeTask(CoreModuleTask coreModuleTask) {
        TimeTask timeTask = new TimeTask(coreModuleTask);
        timeTask.setCreatedAt(LocalDateTime.now());
        timeTask.setCreatedBy("Time trigger");
        timeTask.activeAudienceId1SDeserialize(coreModuleTask.getActiveAudienceId1());
        timeTask.activeAudienceId2SDeserialize(coreModuleTask.getActiveAudienceId2());
        timeTask.audienceId1SDeserialize(coreModuleTask.getAudienceId1());
        timeTask.audienceId2SDeserialize(coreModuleTask.getAudienceId2());
        timeTask.setCoreModuleTask(coreModuleTask);
        timeTask.setTaskStatus(0);
        timeTask.setNodeId(coreModuleTask.getNodeId());
        timeTask.setJourneyId(coreModuleTask.getJourneyId());
        // set make next to 1
        timeTask.setMakenext(1);
        return timeTask;
    }


    @PostMapping("/TimetasktestRepeat")
    public CoreModuleTask TimetasktestRepeat(@RequestBody CoreModuleTask coreModuleTask) {
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
        timeTask.setTriggerTime(date.getTime() + Long.parseLong(flist[0]) * 1000);//Trigger Time = Time now + Delay Time
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

    private void timeParserOnce(String time, CoreModuleTask coreModuleTask) {

        String[] Time_list = time.split(",");
        for(int i=0; i<Time_list.length; i++ ){
            TimeTask timeTask = createTimeTask(coreModuleTask);
            String[] list = Time_list[i].split("T");
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
                Long nodeid = timeTask.getNodeId();
                //nodeRepository.
                if(i>0){
                    Node nodelist = nodeRepository.searchNodeByid(nodeid);
                    Node dummyHeadNode = nodeRepository.searchByJourneyFrontEndIdAndName(nodelist.getJourneyFrontEndId(), "dummyHead");
                    dummyHeadNode.setEndNodesCount(dummyHeadNode.getEndNodesCount()+1);
                    nodeRepository.save(dummyHeadNode);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            timeDelayRepository.save(timeTask);
        }
    }

    private void timeParserRecurring(String frequency, CoreModuleTask coreModuleTask, String endDateStr) {
        String[] list = frequency.split(",");
        LocalDateTime now = LocalDateTime.now();
        Long endDate = parseDateStr(endDateStr);
        for (String dateAndRepeatStr : list) {
            Integer repeatTimes = getRepeatTimes(dateAndRepeatStr);
            // if repeat times <= 0, no need to save new time task
            if (repeatTimes > 0) {
                Long triggerTime = getTriggerTime(dateAndRepeatStr, now);
                // if front end indicates repeatTimes > 0, but after parsing, trigger time > endDate, then no
                // need to save new time tasks, in case: front-end create time trigger recurring at MM/1st, but
                // after 10 days activated, and the end date is MM/2nd, then already passed the endDate.
                if (triggerTime <= endDate) {
                    TimeTask timeTask = createTimeTask(coreModuleTask);
                    timeTask.setTriggerTime(triggerTime);
                    timeTask.setRepeatTimes(repeatTimes);
                    timeDelayRepository.save(timeTask);
                }
            }
        }
    }


    private Long getTriggerTime(String input, LocalDateTime now) {
//        String weekDayString = input.substring(0, input.indexOf(" ") - 4);
        String weekDayString = input.substring(0, input.indexOf(' ')).split("\\d", 2)[0];
        DayOfWeek weekDay = DayOfWeek.valueOf(weekDayString.toUpperCase(Locale.ROOT));

        String timeString = input.substring(weekDayString.length(), input.lastIndexOf(' '));
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("ha", Locale.US);
        LocalTime time = LocalTime.parse(timeString, timeFormatter);

        LocalDateTime triggerTime = now.with(TemporalAdjusters.nextOrSame(weekDay)).with(time);

        if (triggerTime.isBefore(now)) {
            triggerTime = triggerTime.with(TemporalAdjusters.next(weekDay));
        }


        return triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private Long parseDateStr(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);
        LocalDate date = LocalDate.parse(dateStr, formatter);
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private Integer getRepeatTimes(String input) {
        String repeatTimesString = input.substring(input.lastIndexOf(' ') + 1);
        return Integer.parseInt(repeatTimesString);
    }

    private void time_parser_wait_date(String time, TimeTask timeTask) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date parse = format.parse(time + " " + "00:00");
            timeTask.setTriggerTime(parse.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void time_parser_wait_duration(String[] time, TimeTask timeTask) {
        int t = Integer.parseInt(time[0]);
        String unit = time[1];
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime date = null;
        switch (unit) {
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
            } else if (TimeUnit.HOURS.name().equals(timeUnit)) {
                timeTask.setTriggerTime(new Date().getTime() + TimeUnit.HOURS.toMillis(num));
            } else if ("WEEKS".equals(timeUnit)) {
                timeTask.setTriggerTime(new Date().getTime() + TimeUnit.DAYS.toMillis(7 * num));
            }
            timeTask.setRepeatTimes(Integer.parseInt(flist[2]));
            timeTask.setRepeatInterval(flist[3]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @GetMapping("/taskbytime")
    public List<TimeTask> getTaskByTime(@PathVariable(value = "triggerTime", required = false) Long triggerTime) {

        return timeDelayRepository.findTasksBeforeTime(triggerTime);
    }
}