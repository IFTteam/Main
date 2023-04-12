package springredis.demo.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import springredis.demo.controller.EventWebhookController;
import springredis.demo.controller.TimeEventController;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.repository.*;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.tasks.CMTExecutor;

import java.time.LocalDate;
import java.util.*;

@Service
public class IfElseTaskServiceImpl implements IfElseTaskService {


    @Autowired
    private AudienceRepository audienceRepository;
    @Autowired
    private ActiveAudienceRepository activeAudienceRepository;

    @Autowired
    private JourneyRepository journeyRepository;
    @Autowired
    NodeRepository NodeRepository;
    @Autowired
    private TransmissionRepository transmissionRepository;

    @Autowired
    private AudienceActivityRepository audienceActivityRepository;

    @Autowired
    private EventWebhookController eventWebhookController;

    @Autowired
    private CMTExecutor cmtExecutor;

    @Autowired
    private TimeEventController timeEventController;

    @Override
    public CoreModuleTask filterByAudienceAction(CoreModuleTask coreModuleTask) throws JsonProcessingException {
        Set<Audience> allAudience = new HashSet<>();

        // Get httpEntity from Name
        // {'repeatInterval': 'XXX', 'repeat': #, 'triggerTime': #, 'eventType': 'WWW', 'httpEntity': [{'aaa'},{'bbb'}, ... ,{'ccc'}]};
        String json_text = coreModuleTask.getName();
        String new_text = json_text.substring(1, json_text.length() - 1);
        String httpEntityText = new_text.substring(new_text.indexOf("httpEntity") + 15, new_text.length() - 2);
        String[] a = httpEntityText.split("},\\{");
        List<HttpEntity<String>> httpEntity = new ArrayList<>();
        for(String i : a) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            HttpEntity<String> entity = new HttpEntity<>(i, headers);
            httpEntity.add(entity);
        }
        String without_httpEntity = new_text.substring(0, new_text.indexOf("httpEntity") -3);
        String[] items = without_httpEntity.split(", ");



        String repeatInterval = items[0].substring(19, items[0].length() - 1);
        int repeat = Integer.parseInt(items[1].substring(10, items[1].length()));
        int triggerTime = Integer.parseInt(items[2].substring(15, items[2].length()));
        String eventType = items[3].substring(14, items[3].length() - 1);

        Long userId = coreModuleTask.getUserId();
        Long nodeId = coreModuleTask.getNodeId();
        Long targetNodeId = coreModuleTask.getTargetNodeId();
        Long journeyId = coreModuleTask.getJourneyId();

        Optional<Journey> journey = journeyRepository.findById(journeyId);
        //need to fix
        List<Transmission> transmissionList = transmissionRepository.findAll();





        for (HttpEntity<String> item : httpEntity) {
            // handleEventWebhook method will insert audiences to table automatically
            // respond to incoming webhook, uses transmission id to query transmission entity, get audience_id
            // and audience_email. Then insert into audience_activity table
            ResponseEntity<Response> justCall= eventWebhookController.handleEventWebhook(item);

            // find corresponding audience
            String json = item.getBody();
            Event event = new ObjectMapper().readerFor(Event.class).readValue(json);
            String transmissionId = event.getMsys().getEventDetail().getTransmissionId();
            Optional<Transmission> transmission = transmissionRepository.findById(Long.valueOf(transmissionId));

            if (transmissionList.contains(transmission)) {
                Audience audience = transmission.get().getAudience();
                allAudience.add(audience);
            }


        }


//builder().b("b").a("a").build();
        BaseTaskEntity taskEntity = new BaseTaskEntity();
        taskEntity.setNodeId(nodeId);
        taskEntity.setJourneyId(journeyId);
        taskEntity.setUserId(userId);
        taskEntity.setTargetNodeId(targetNodeId);

        // Start time counting

        TimeTask task = timeEventController.add(taskEntity);

        task.setRepeatInterval(repeatInterval);
        task.setRepeatTimes(repeat);
        task.setTriggerTime((long) triggerTime);
//        EventType{
//        delivery,
//                click,
//                open,
//                list_unsubscribe,
//                link_unsubscribe,
//                bounce
//        }


        Set<Audience> haveBehavior =new HashSet<>();
        Set<Audience> restAudience = new HashSet<>(allAudience);

        while (task.getTaskStatus() == 0) {
            for (Audience audience: allAudience) {
                AudienceActivity audienceActivity = audienceActivityRepository.getAudienceActivityByAudience(audience);
                String currentType = audienceActivity.getEventType();
                if (currentType.equals(eventType)) {
                    CoreModuleTask newTask = coreModuleTask;
                    List<Long> audienceList1 = new ArrayList<>();
                    audienceList1.add(audience.getId());
                    newTask.setAudienceId1(audienceList1);
                    newTask.setAudienceId2(new ArrayList<>());
                    newTask.setCallapi(0);                      //jiaqi: important, because when calling the CMTexecutor again with this task, we don't want it to call back to our if/else controller again since this trigger has already hit
                    newTask.setMakenext(1);
                    newTask.setTaskType(0);                     //the audience must already be in our main DB, so we move a user (audience), not create one
                    cmtExecutor.execute(newTask);
                    // TODO: TaskController (done)
                    restAudience.remove(audience);
                    audienceActivityRepository.delete(audienceActivity);
                }
            }
        }
        CoreModuleTask newTask = coreModuleTask;
        List<Long> audienceList1 = new ArrayList<>();
        newTask.setAudienceId1 (audienceList1);
        List<Long> audienceList2 = new ArrayList<>();
        for (Audience audience : restAudience) {
            Long Id = audience.getId();
            audienceList2.add(Id);
        }
        newTask.setAudienceId2 (audienceList2);
        newTask.setCallapi(0);                      //jiaqi: important, because when calling the CMTexecutor again with this task, we don't want it to call back to our if/else controller again since this trigger has already hit
        newTask.setMakenext(1);
        newTask.setTaskType(0);                     //the audience must already be in our main DB, so we move a user (audience), not create one
        //这里没有再用cmtexectutor执行newtask是因为这个newtask被return后应该会在call这个api的task coordinator的cmtexecutor中继续进行下一步的执行（transfer audience，etc）
        return newTask;
    }

    @Override
    // public String ifElseProperty(List<Audience> listOfAudiences, String property, String condition, String value) {
    public CoreModuleTask ifElseProperty(CoreModuleTask coreModuleTask) {

        List<Audience> haveProperty = new ArrayList<>();

        List<Long> listOfAudienceId = coreModuleTask.getActiveAudienceId1();
        System.out.println("size of active Audience Id1: "+listOfAudienceId.size());

        List<Audience> listOfAudiences = new ArrayList<>();
        for (Long id : listOfAudienceId) {
            //Audience audience = audienceRepository.findById(id).get();

            // active_audience := id
            ActiveAudience activeAudience = activeAudienceRepository.findById(id).get();
            // active_audience := audience_id
            Audience audience = audienceRepository.findById(activeAudience.getAudienceId()).get();

            listOfAudiences.add(audience);
        }

        Node node = NodeRepository.searchNodeByid(coreModuleTask.getNodeId());
        String json_text = node.getProperties();

        String marker1 = "property";
        String marker2 = "condition";
        String marker3 = "value";
        String property = "";
        String condition = "";
        String value = "";
        int indexOfMarker1  = json_text.indexOf(marker1);
        int indexOfMarker2  = json_text.indexOf(marker2);
        int indexOfMarker3  = json_text.indexOf(marker3);
        property = json_text.substring(indexOfMarker1 + marker1.length() + 4, indexOfMarker2 - 6);
        condition = json_text.substring(indexOfMarker2 + marker2.length() + 4, indexOfMarker3 - 6);
        value = json_text.substring(indexOfMarker3 + marker3.length() + 4, json_text.length() - 3);

        System.out.println("property: "+property);
        System.out.println("condition: "+condition);
        System.out.println("value: "+value);

//        for( Audience audience: listOfAudiences) {
//            String audienceProperty = audience.toString();
//            audienceProperty = audienceProperty.substring(0, audienceProperty.length() - 1);
//            List<String> items = Arrays.asList(audienceProperty.split("\\s*,\\s*"));
//            String find = "first_name";
//            String substr = "";
//            for (String item : items) {
//                int i = item.indexOf(find);
//                if( i >= 0 ) {
//                    substr = item.substring(i+find.length()+2, item.length() - 1);
//                    if (substr == condition) {
//                        haveProperty.add(audience);
//                    }
//                }
//            }
//        }



//        private String source;


// -------------------------------------- address --------------------------------------
        if (property.equals("Location")) {
            if (condition.equals("Contains")) {
                for(Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        continue;
                    }
                    if (audience.getAddress().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Does Not Contain")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        continue;
                    }
                    if (!audience.getAddress().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Is in Country")) {
                for(Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        continue;
                    }
                    if (audience.getAddress().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Is Not in Country")) {
                for(Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        continue;
                    }
                    if (!audience.getAddress().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Is In US State")) {
                for(Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        continue;
                    }
                    if (audience.getAddress().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Is Not In US State")) {
                for(Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        continue;
                    }
                    if (!audience.getAddress().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

        }

// -------------------------------------- birthday --------------------------------------
        if (property.equals("Birthday")) {
            if (condition.equals("Month is")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getBirthday() == null) {
                        continue;
                    }
                    if (audience.getBirthday().getMonth().toString().equalsIgnoreCase(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Date is")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getBirthday() == null) {
                        continue;
                    }
                    if (Integer.toString(audience.getBirthday().getDayOfMonth()).equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is (mm.dd)")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getBirthday() == null) {
                        continue;
                    }

                    int input_month = Integer.parseInt(value.substring(0, 2));
                    int input_day = Integer.parseInt(value.substring(3, 5));
                    int real_month = audience.getBirthday().getMonth().getValue();
                    int real_day = audience.getBirthday().getDayOfMonth();

                    if (input_month == real_month && input_day == real_day) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is After Date")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getBirthday() == null) {
                        continue;
                    }

                    int input_year = Integer.parseInt(value.substring(0, 4));
                    int input_month = Integer.parseInt(value.substring(5, 7));
                    int input_day = Integer.parseInt(value.substring(8, 10));

                    LocalDate localDate = LocalDate.of(input_year,input_month,input_day);
                    if (audience.getBirthday().compareTo(localDate) > 0) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is Before Date")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getBirthday() == null) {
                        continue;
                    }

                    int input_year = Integer.parseInt(value.substring(0, 4));
                    int input_month = Integer.parseInt(value.substring(5, 7));
                    int input_day = Integer.parseInt(value.substring(8, 10));

                    LocalDate localDate = LocalDate.of(input_year,input_month,input_day);
                    if (audience.getBirthday().compareTo(localDate) < 0) {
                        haveProperty.add(audience);
                    }
                }
            }
        }


// -------------------------------------- email address --------------------------------------
        if (property.equals("Email Address")) {
            if (condition.equals("is")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getEmail() == null) {
                        continue;
                    }
                    if (audience.getEmail().equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getEmail() == null) {
                        continue;
                    }
                    if (!audience.getEmail().equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Contains")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getEmail() == null) {
                        continue;
                    }
                    if (audience.getEmail().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Does Not Contain")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getEmail() == null) {
                        continue;
                    }
                    if (!audience.getEmail().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("starts with")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getEmail() == null) {
                        continue;
                    }
                    if (audience.getEmail().startsWith(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("ends with")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getEmail() == null) {
                        continue;
                    }
                    if (audience.getEmail().endsWith(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            // 1 > 2 --> +
            // 1 < 2 --> -
            // audience's email greater than input
            if (condition.equals("is greater than")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getEmail() == null) {
                        continue;
                    }
                    if (audience.getEmail().compareTo(value) > 0){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is less than")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getEmail() == null) {
                        continue;
                    }
                    if (audience.getEmail().compareTo(value) < 0){
                        haveProperty.add(audience);
                    }
                }
            }


        }


// -------------------------------------- first name --------------------------------------
        if (property.equals("First Name")) {
            if (condition.equals("is")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getFirstName() == null) {
                        continue;
                    }
                    if (audience.getFirstName().equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getFirstName() == null) {
                        continue;
                    }
                    if (!audience.getFirstName().equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Contains")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getFirstName() == null) {
                        continue;
                    }
                    if (audience.getFirstName().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Does Not Contain")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() == null) {
                        continue;
                    }
                    if (!audience.getFirstName().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("starts with")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() == null) {
                        continue;
                    }
                    if (audience.getFirstName().startsWith(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("ends with")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() == null) {
                        continue;
                    }
                    if (audience.getFirstName().endsWith(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is greater than")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() == null) {
                        continue;
                    }
                    if (audience.getFirstName().compareTo(value) > 0){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is less than")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() == null) {
                        continue;
                    }
                    if (audience.getFirstName().compareTo(value) < 0){
                        haveProperty.add(audience);
                    }
                }
            }

        }

// -------------------------------------- last name --------------------------------------
        if (property.equals("Last Name")) {
            if (condition.equals("is")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getLastName() == null) {
                        continue;
                    }
                    if (audience.getLastName().equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getLastName() == null) {
                        continue;
                    }
                    if (!audience.getLastName().equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Contains")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getLastName() == null) {
                        continue;
                    }
                    if (audience.getLastName().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Does Not Contain")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getLastName() == null) {
                        continue;
                    }
                    if (!audience.getLastName().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("starts with")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getLastName() == null) {
                        continue;
                    }
                    if (audience.getLastName().startsWith(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("ends with")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getLastName() == null) {
                        continue;
                    }
                    if (audience.getLastName().endsWith(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is greater than")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getLastName() == null) {
                        continue;
                    }
                    if (audience.getLastName().compareTo(value) > 0){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is less than")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getLastName() == null) {
                        continue;
                    }
                    if (audience.getLastName().compareTo(value) < 0){
                        haveProperty.add(audience);
                    }
                }
            }

        }


// -------------------------------------- full name --------------------------------------
        if (property.equals("Full Name")) {
            if (condition.equals("is")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getFirstName() == null && audience.getLastName() == null) {
                        continue;
                    }

                    String fullname = audience.getFirstName()+" "+audience.getLastName();
                    if (fullname.equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getFirstName() == null && audience.getLastName() == null) {
                        continue;
                    }

                    String fullname = audience.getFirstName()+" "+audience.getLastName();
                    if (!fullname.equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Contains")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getFirstName() == null && audience.getLastName() == null) {
                        continue;
                    }

                    String fullname = audience.getFirstName()+" "+audience.getLastName();
                    if (fullname.contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Does Not Contain")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() == null && audience.getLastName() == null) {
                        continue;
                    }

                    String fullname = audience.getFirstName()+" "+audience.getLastName();
                    if (!fullname.contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

        }


// -------------------------------------- phone number --------------------------------------
        if (property.equals("Phone Number")) {
            if (condition.equals("is")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getPhone() == null) {
                        continue;
                    }
                    if (audience.getPhone().equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getPhone() == null) {
                        continue;
                    }
                    if (!audience.getPhone().equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Contains")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getPhone() == null) {
                        continue;
                    }
                    if (audience.getPhone().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("Does Not Contain")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getPhone() == null) {
                        continue;
                    }
                    if (!audience.getPhone().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("starts with")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getPhone() == null) {
                        continue;
                    }
                    if (audience.getPhone().startsWith(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("ends with")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getPhone() == null) {
                        continue;
                    }
                    if (audience.getPhone().endsWith(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is greater than")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getPhone() == null) {
                        continue;
                    }
                    if (audience.getPhone().compareTo(value) > 0){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is less than")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getPhone() == null) {
                        continue;
                    }
                    if (audience.getPhone().compareTo(value) < 0){
                        haveProperty.add(audience);
                    }
                }
            }

        }

// -------------------------------------- date added --------------------------------------
// input format: "XXXX-XX-XX"

        if (property.equals("date added")) {
            // selected date is > value
            if (condition.equals("is after")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getDate_added() == null) {
                        continue;
                    }
                    LocalDate localDate = LocalDate.parse(value);
                    if (audience.getDate_added().compareTo(localDate) > 0) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is before")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getDate_added() == null) {
                        continue;
                    }
                    LocalDate localDate = LocalDate.parse(value);
                    if (audience.getDate_added().compareTo(localDate) < 0) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getDate_added() == null) {
                        continue;
                    }
                    LocalDate localDate = LocalDate.parse(value);
                    if (audience.getDate_added().compareTo(localDate) == 0) {
                        haveProperty.add(audience);
                    }
                }
            }


            if (condition.equals("is within")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getDate_added() == null) {
                        continue;
                    }
                    LocalDate now = LocalDate.now();
                    if (audience.getDate_added().compareTo(now.minusDays(Integer.parseInt(value))) >= 0){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not within")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getDate_added() == null) {
                        continue;
                    }
                    LocalDate now = LocalDate.now();
                    if (audience.getDate_added().compareTo(now.minusDays(Integer.parseInt(value))) < 0){
                        haveProperty.add(audience);
                    }
                }
            }


        }
// -------------------------------------- source --------------------------------------
        if (property.equals("signup source")) {
            if (condition.equals("was")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getSource() == null) {
                        continue;
                    }
                    if (audience.getSource().equalsIgnoreCase(value)){
                        haveProperty.add(audience);
                    }
                }

            }

            if (condition.equals("was not")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getSource() == null) {
                        continue;
                    }
                    if (!audience.getSource().equalsIgnoreCase(value)){
                        haveProperty.add(audience);
                    }
                }
            }

        }


        // -------------------------------------- gender --------------------------------------
        if (property.equals("Gender")) {
            if (condition.equals("is")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getGender() == null) {
                        continue;
                    }
                    if (audience.getGender().equals(value)) {
                        haveProperty.add(audience);
                    }
                }
            }
        }



        List<Audience> noProperty = listOfAudiences;
        noProperty.removeAll(haveProperty);

        System.out.println("________________audiencelist1: ");
        List<Long> audienceList1 = new ArrayList<>();
        for (Audience audience : haveProperty) {
            Long Id = audience.getId();
            audienceList1.add(Id);
            System.out.print(" "+Id);
        }
        System.out.println();

        System.out.println("________________audiencelist2: ");
        List<Long> audienceList2 = new ArrayList<>();
        for (Audience audience : noProperty) {
            Long Id = audience.getId();
            audienceList2.add(Id);
            System.out.print(" "+Id);
        }
        System.out.println();

        CoreModuleTask newTask = coreModuleTask;
        newTask.setAudienceId1(audienceList1);
        newTask.setAudienceId2(audienceList2);
        //jiaqi:这里为了确保task的状态，还是要手动的set一下makenext为1来确保return之后下一个task会被task coordinator的CMTexecutor制作
        newTask.setMakenext(1);
        newTask.setTaskType(1);
        return newTask;
    }



    @Override
    public CoreModuleTask ifElsePropertyWithoutValue(CoreModuleTask coreModuleTask) {
        List<Audience> haveProperty = new ArrayList<>();

        List<Long> listOfAudienceId = coreModuleTask.getActiveAudienceId1();
        List<Audience> listOfAudiences = new ArrayList<>();
        for (Long id : listOfAudienceId) {
            Audience audience = audienceRepository.findById(id).get();
            listOfAudiences.add(audience);
        }

        Node node = NodeRepository.searchNodeByid(coreModuleTask.getNodeId());
        String json_text = node.getProperties();

        String marker1 = "property";
        String marker2 = "condition";
        String property = "";
        String condition = "";
        int indexOfMarker1  = json_text.indexOf(marker1);
        int indexOfMarker2  = json_text.indexOf(marker2);
        property = json_text.substring(indexOfMarker1 + marker1.length() + 4, indexOfMarker2 - 6);

        // todo: 目前json text没有“value”这一项，但要考虑json text为null的情况
        condition = json_text.substring(indexOfMarker2 + marker2.length() + 4, json_text.length() - 3);

        /*
        String new_text = json_text.substring(1, json_text.length() - 1);
        String[] items = new_text.split(", ");
        String property = items[0].substring(13, items[0].length() - 1);
        String condition = items[1].substring(14, items[1].length() - 1);
        */
        System.out.println("property: "+property);
        System.out.println("condition: "+condition);



        if (property.equals("Location")) {
            if (condition.equals("is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getAddress() != null) {
                        haveProperty.add(audience);
                    }
                }
            }
        }

        if (property.equals("Birthday")) {
            if (condition.equals("is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getBirthday() == null) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getBirthday() != null) {
                        haveProperty.add(audience);
                    }
                }
            }
        }

        if (property.equals("First Name")) {
            if (condition.equals("is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() == null) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() != null) {
                        haveProperty.add(audience);
                    }
                }
            }
        }


        if (property.equals("Last Name")) {
            if (condition.equals("is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getLastName() == null) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getLastName() != null) {
                        haveProperty.add(audience);
                    }
                }
            }
        }

        if (property.equals("Full Name")) {
            if (condition.equals("is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() == null && audience.getLastName() == null) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() != null || audience.getLastName() != null) {
                        haveProperty.add(audience);
                    }
                }
            }
        }

        if (property.equals("Phone Number")) {
            if (condition.equals("is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getPhone() == null) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("is not blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getPhone() != null) {
                        haveProperty.add(audience);
                    }
                }
            }
        }

        if (property.equals("Gender")) {
            if (condition.equals("is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getGender() == null) {
                        haveProperty.add(audience);
                    }
                }
            }
        }



        List<Audience> noProperty = listOfAudiences;
        noProperty.removeAll(haveProperty);

        System.out.println("________________audiencelist1: ");
        List<Long> audienceList1 = new ArrayList<>();
        for (Audience audience : haveProperty) {
            Long Id = audience.getId();
            System.out.print(" "+Id);
            audienceList1.add(Id);
        }
        System.out.println();

        System.out.println("________________audiencelist2: ");
        List<Long> audienceList2 = new ArrayList<>();
        for (Audience audience : noProperty) {
            Long Id = audience.getId();
            audienceList2.add(Id);
            System.out.print(" "+Id);
        }
        System.out.println();

        CoreModuleTask newTask = coreModuleTask;
        newTask.setAudienceId1(audienceList1);
        newTask.setAudienceId2(audienceList2);
        //jiaqi:这里为了确保task的状态，还是要手动的set一下makenext为1来确保return之后下一个task会被task coordinator的CMTexecutor制作
        newTask.setMakenext(1);
        newTask.setTaskType(1);
        return newTask;
    }
}
