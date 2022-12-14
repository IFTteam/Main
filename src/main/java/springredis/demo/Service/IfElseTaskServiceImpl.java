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
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.repository.AudienceActivityRepository;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.JourneyRepository;
import springredis.demo.repository.TransmissionRepository;
import springredis.demo.tasks.CMTExecutor;

import java.time.LocalDate;
import java.util.*;

@Service
public class IfElseTaskServiceImpl implements IfElseTaskService {


    @Autowired
    private AudienceRepository audienceRepository;

    @Autowired
    private JourneyRepository journeyRepository;

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
        List<Transmission> transmissionList = transmissionRepository.getTransmissionByJourney(journey);





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
        //??????????????????cmtexectutor??????newtask???????????????newtask???return???????????????call??????api???task coordinator???cmtexecutor????????????????????????????????????transfer audience???etc???
        return newTask;
    }

    @Override
    // public String ifElseProperty(List<Audience> listOfAudiences, String property, String condition, String value) {
    public CoreModuleTask ifElseProperty(CoreModuleTask coreModuleTask) {

        List<Audience> haveProperty = new ArrayList<>();

        List<Long> listOfAudienceId = coreModuleTask.getActiveAudienceId1();
        List<Audience> listOfAudiences = new ArrayList<>();
        for (Long id : listOfAudienceId) {
            Audience audience = audienceRepository.findById(id).get();
            listOfAudiences.add(audience);
        }


        String json_text = coreModuleTask.getName();
        String new_text = json_text.substring(1, json_text.length() - 1);
        String[] items = new_text.split(", ");
        String property = items[0].substring(13, items[0].length() - 1);
        String condition = items[1].substring(14, items[1].length() - 1);
        String value = items[2].substring(10, items[2].length() - 1);

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
        if (property.equals("address")) {
            if (condition.equals("contains")) {
                for(Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        continue;
                    }
                    if (audience.getAddress().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("does not contains")) {
                for( Audience audience: listOfAudiences) {
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
        if (property.equals("birthday")) {
            if (condition.equals("month is")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getBirthday() == null) {
                        continue;
                    }
                    if (audience.getBirthday().getMonth().toString().equalsIgnoreCase(value)) {
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("day is")) {
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
        }


// -------------------------------------- email address --------------------------------------
        if (property.equals("email address")) {
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

            if (condition.equals("contains")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getEmail() == null) {
                        continue;
                    }
                    if (audience.getEmail().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("does not contains")) {
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
        if (property.equals("first name")) {
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

            if (condition.equals("contains")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getFirstName() == null) {
                        continue;
                    }
                    if (audience.getFirstName().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("does not contains")) {
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
        if (property.equals("last name")) {
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

            if (condition.equals("contains")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getLastName() == null) {
                        continue;
                    }
                    if (audience.getLastName().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("does not contains")) {
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

// -------------------------------------- phone number --------------------------------------
        if (property.equals("phone number")) {
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

            if (condition.equals("contains")) {
                for (Audience audience : listOfAudiences) {
                    if (audience.getPhone() == null) {
                        continue;
                    }
                    if (audience.getPhone().contains(value)){
                        haveProperty.add(audience);
                    }
                }
            }

            if (condition.equals("does not contains")) {
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



        List<Audience> noProperty = listOfAudiences;
        noProperty.removeAll(haveProperty);

        List<Long> audienceList1 = new ArrayList<>();
        for (Audience audience : haveProperty) {
            Long Id = audience.getId();
            audienceList1.add(Id);
        }

        List<Long> audienceList2 = new ArrayList<>();
        for (Audience audience : noProperty) {
            Long Id = audience.getId();
            audienceList2.add(Id);
        }

        CoreModuleTask newTask = coreModuleTask;
        newTask.setAudienceId1(audienceList1);
        newTask.setAudienceId2(audienceList2);
        //jiaqi:??????????????????task??????????????????????????????set??????makenext???1?????????return???????????????task??????task coordinator???CMTexecutor??????
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


        String json_text = coreModuleTask.getName();
        String new_text = json_text.substring(1, json_text.length() - 1);
        String[] items = new_text.split(", ");
        String property = items[0].substring(13, items[0].length() - 1);
        String condition = items[1].substring(14, items[1].length() - 1);



        if (property.equals("address")) {
            if (condition.equals("is blank")) {
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

        if (property.equals("birthday")) {
            if (condition.equals("is blank")) {
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

        if (property.equals("first name")) {
            if (condition.equals("is blank")) {
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


        if (property.equals("last name")) {
            if (condition.equals("is blank")) {
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

        if (property.equals("phone number")) {
            if (condition.equals("is blank")) {
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



        List<Audience> noProperty = listOfAudiences;
        noProperty.removeAll(haveProperty);


        List<Long> audienceList1 = new ArrayList<>();
        for (Audience audience : haveProperty) {
            Long Id = audience.getId();
            audienceList1.add(Id);
        }

        List<Long> audienceList2 = new ArrayList<>();
        for (Audience audience : noProperty) {
            Long Id = audience.getId();
            audienceList2.add(Id);
        }


        CoreModuleTask newTask = coreModuleTask;
        newTask.setAudienceId1(audienceList1);
        newTask.setAudienceId2(audienceList2);
        //jiaqi:??????????????????task??????????????????????????????set??????makenext???1?????????return???????????????task??????task coordinator???CMTexecutor??????
        newTask.setMakenext(1);
        newTask.setTaskType(1);
        return newTask;
    }
}
