package springredis.demo.structures;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import springredis.demo.entity.Event;
import springredis.demo.entity.TimeTask;
import springredis.demo.entity.User;
import springredis.demo.repository.TimeDelayRepository;
import springredis.demo.repository.UserRepository;
import springredis.demo.controller.API_Trigger_Controller;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SimulateNewEvent {


    private final TimeDelayRepository timeDelayRepository;
    private final UserRepository userRepository;
    private final RedisTemplate redisTemplate;
    private ScheduledFuture<?> scheduledFuture;

    private final API_Trigger_Controller apiTriggerController;
    @Autowired
    RestTemplate restTemplate;

    /**
     * auto-wired taskScheduler to do scheduling task
     */
    private final TaskScheduler taskScheduler;

    /**
     * cron Expression to determine current running status
     */
    @Setter
    @Getter
    @Value("${corn.expressions.everyTenSecond}")
    private String cronExpression;

    @Setter
    @Getter
    @Value("${corn.expressions.everyThirtyMins}")
    private String AbandonExpression;

    @Value("${redis-key.in-queue-key}")
    private String inQueueKey;


    @Autowired
    public SimulateNewEvent(TimeDelayRepository timeDelayRepository, UserRepository userRepository, RedisTemplate redisTemplate, API_Trigger_Controller apiTriggerController, TaskScheduler taskScheduler) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.timeDelayRepository = timeDelayRepository;
        this.apiTriggerController = apiTriggerController;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        log.info("start SimulateNewEvent...");
        startSimulateNewEvent(cronExpression);
        startSimulateNewAbandonEvent(AbandonExpression);
    }

    /**
     * start processSimulateNewEvent() with given cronExpression
     * @param cronExpression given cronExpression
     */
    public void startSimulateNewEvent(String cronExpression) {
        log.info("start running SimulateNewEvent for {} ...", cronExpression);
        Runnable task = this::processSimulateNewEvent;
        setCronExpression(cronExpression);
        scheduledFuture = taskScheduler.schedule(task, new CronTrigger(cronExpression));
    }

    public void startSimulateNewAbandonEvent(String AbandonExpression) {
        log.info("start running SimulateNewAbadonEvent for {} ...", AbandonExpression);
        Runnable task = this::processSimulateNewAbandonEvent;
        setCronExpression(AbandonExpression);
        scheduledFuture = taskScheduler.schedule(task, new CronTrigger(AbandonExpression));
    }

    /**
     * stop processSimulateNewEvent()
     */
    @PreDestroy
    public void stopSimulateNewEvent() {
        log.info("stop running SimulateNewEvent...");
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(true);
        }
    }

    public void processSimulateNewEvent() {
        int timeAhead = 0;//the time before the task trigger that we bring a task from sql to redis(ms)
        Date time = new Date();
        System.out.println(time.getTime());
        List<TimeTask> timeTasks = timeDelayRepository.findTasksBeforeTime(time.getTime() + timeAhead);
        for (TimeTask timeTask : timeTasks) {
            log.info("caught a triggered time task...");
            time.setTime(timeTask.getTriggerTime());
            Event event = new Event(time, timeTask.getId());
            // repeatTimes - 1
            timeTask.setRepeatTimes(timeTask.getRepeatTimes() - 1);
            // if repeatTimes still > 0, then set new trigger time -> current trigger time +
            // one week.
            // if repeatTimes <= 0, then just set task status to 1
            if (timeTask.getRepeatTimes() > 0) {
                timeTask.setTriggerTime(timeTask.getTriggerTime() + getOneWeekInMilliseconds());
            }
            else {
                timeTask.setTaskStatus(1);
            }
            timeDelayRepository.save(timeTask);

            redisTemplate.opsForList().leftPush(inQueueKey, event);
            System.out.println("Insert New Event at" + time);
        }
    }

    public void processSimulateNewAbandonEvent() {
        Date time = new Date();
        //get the timestamp half an hour ago
        long searchTime = time.getTime()+30*60*1000;
        System.out.println(searchTime);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        String sd = format.format(new Date(Long.parseLong(String.valueOf(searchTime))));
        List<User> userLists = userRepository.findAll();
        for (User userList : userLists) {
            log.info("abandon checkout");
            String shopifyApiKey = userList.getShopifyApiKey();
            String shopifydevstore = userList.getShopifydevstore();
            String shopifyApiAccessToken = userList.getShopifyApiAccessToken();
            Long userid = userList.getId();
            String url = "https://"+shopifydevstore+".myshopify.com/admin/api/2023-04/checkouts.json?created_at_min="+sd;
            System.out.println(url);
            HttpHeaders header = new HttpHeaders();
            header.set("X-Shopify-Access-Token", shopifyApiAccessToken);
            header.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity("{}", header);
            ResponseEntity<String > response = restTemplate.exchange(url, HttpMethod.GET,request, String.class);
            JSONObject ResJson = new JSONObject(response);
            //body = "{"checkouts":[{"buyer_accepts_marketing":false,"cart_token":"c1-9284d593b099fd3f9c6df6ad6e7f01e7","closed_at":null,"completed_at":null,"created_at":"2023-08-31T15:09:31-04:00","currency":"USD","presentment_currency":"USD","customer_locale":"en-US","device_id":null,"email":"Chrisxu95@gmail.com","gateway":null,"id":36928110723351,"landing_site":"\/75932172567\/digital_wallets\/dialog","location_id":null,"note":null,"note_attributes":[],"phone":null,"referring_site":"https:\/\/quick-start-35b2b64d.myshopify.com\/","source_identifier":null,"source_url":null,"taxes_included":false,"token":"ff433abd853adde761f9c77091dc2603","total_weight":0,"updated_at":"2023-09-01T01:10:01-04:00","user_id":null,"customer":{"id":7267178414359,"email":"chrisxu95@gmail.com","accepts_marketing":false,"created_at":"2023-08-15T15:11:46-04:00","updated_at":"2023-08-31T15:09:31-04:00","first_name":"Huanting","last_name":"Xu","orders_count":14,"state":"disabled","total_spent":"0.00","last_order_id":5489397727511,"no"... View
            JSONObject JsonBody = new JSONObject(ResJson.get("body").toString());
            JSONArray checkouts = JsonBody.getJSONArray("checkouts");
            for (int i = 0; i < checkouts.length(); i++) {
                String jsonStr = checkouts.getJSONObject(i).toString();
                apiTriggerController.shopifyAbandonCartTriggerHit( userid.toString(),"",jsonStr);

            }
        }
    }

    private static Long getOneWeekInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS);
    }
}