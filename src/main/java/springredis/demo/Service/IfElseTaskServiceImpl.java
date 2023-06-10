package springredis.demo.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import springredis.demo.controller.EventWebhookController;
import springredis.demo.controller.TimeEventController;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.repository.*;
import springredis.demo.repository.WorldCityRepository;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.tasks.CMTExecutor;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class IfElseTaskServiceImpl implements IfElseTaskService {


    @Autowired
    private AudienceRepository audienceRepository;
    @Autowired
    private ActiveAudienceRepository activeAudienceRepository;

    @Autowired
    private WorldCityRepository worldCityRepository;
    @Autowired
    private JourneyRepository journeyRepository;
    @Autowired
    NodeRepository NodeRepository;
    @Autowired
    private TransmissionRepository transmissionRepository;

    @Autowired
    private AudienceActivityRepository audienceActivityRepository;

    @Autowired
    AudienceListRepository audienceListRepository;

    @Autowired
    private EventWebhookController eventWebhookController;

    @Autowired
    private CMTExecutor cmtExecutor;

    @Autowired
    private TimeEventController timeEventController;


    @Override
    public CoreModuleTask filterByAudienceAction(CoreModuleTask coreModuleTask) throws JsonProcessingException {

        /*
        List <AudienceList> AudienceListList = audienceListRepository.findAll();
        for (AudienceList audiencelist : AudienceListList)
        {
            audiencelist.removeAudience(audienceRepository.searchAudienceByid(19));
            audienceListRepository.save(audiencelist);
        }
         */

        // Get the active audience list
        List<Long> listOfActiveAudienceId = coreModuleTask.getActiveAudienceId1();
        List<Long> listOfAudienceId = new ArrayList<>();
        List<Audience> listOfAudiences = new ArrayList<>();
        for (Long id : listOfActiveAudienceId) {
            // active_audience := id
            ActiveAudience activeAudience = activeAudienceRepository.findById(id).get();

            // active_audience := audience_id
            Audience audience = audienceRepository.findById(activeAudience.getAudienceId()).get();
            listOfAudiences.add(audience);

            listOfAudienceId.add(audience.getId());
        }

        // {"property": "opened", "condition": "in 1 hour(s)","value" : "campaign 1"}
        // {'repeatInterval': 'XXX', 'repeat': #, 'triggerTime': #, 'eventType': 'WWW', 'httpEntity': [{'aaa'},{'bbb'}, ... ,{'ccc'}]};

        Node node = NodeRepository.searchNodeByid(coreModuleTask.getNodeId());
        String json_text = node.getProperties();

        // todo: parsing需要对应前端的修改
        // parse the property, condition, and value
        // {"property": "opened", "condition": "in 1 hour(s)","value" : "campaign 1"}
        String marker1 = "property"; String marker2 = "condition"; String marker3 = "value";
        String property = "";
        String condition = "";
        String value = "";
        int indexOfMarker1  = json_text.indexOf(marker1);
        int indexOfMarker2  = json_text.indexOf(marker2);
        int indexOfMarker3  = json_text.indexOf(marker3);
        property = json_text.substring(indexOfMarker1 + marker1.length() + 4, indexOfMarker2 - 6);
        condition = json_text.substring(indexOfMarker2 + marker2.length() + 4, indexOfMarker3 - 7);
        value = json_text.substring(indexOfMarker3 + marker3.length() + 4, json_text.length() - 3);

        System.out.println("property: "+property);
        System.out.println("condition: "+condition);
        System.out.println("value: "+value);

        String[] parsedTriggerTime = condition.split(" ");
        String unit = parsedTriggerTime[2];

        Long userId = coreModuleTask.getUserId();
        Long nodeId = coreModuleTask.getNodeId();
        Long targetNodeId = coreModuleTask.getTargetNodeId();
        Long journeyId = coreModuleTask.getJourneyId();

        Optional<Journey> journey = journeyRepository.findById(journeyId);
        //Optional<Transmission> transmission = transmissionRepository.findById(transmissionId);

        /*
        // todo: time task 备份
        BaseTaskEntity taskEntity = new BaseTaskEntity();
        taskEntity.setNodeId(nodeId);
        taskEntity.setJourneyId(journeyId);
        taskEntity.setUserId(userId);
        taskEntity.setTargetNodeId(targetNodeId);

        // Start time counting
        TimeTask task = timeEventController.add(taskEntity);

        // timeValue: An integer representing the new timestamp — the number of milliseconds since the midnight at the beginning of January 1, 1970, UTC.
        // 这里设置的是当前时间后的5分钟
        long triggerTime = System.currentTimeMillis() + 5 *  60 * 1000;
        task.setRepeatInterval("once");
        task.setRepeatTimes(1);
        task.setTriggerTime(triggerTime);
        */

//        EventType{
//        delivery,
//                click,
//                open,
//                list_unsubscribe,
//                link_unsubscribe,
//                bounce
//        }

        Set<Audience> allAudience = new HashSet<>();
        Set<Audience> haveBehavior =new HashSet<>();
        Set<Audience> restAudience = new HashSet<>(listOfAudiences);

        List<Long> audienceList1 = new ArrayList<>();

            for (Audience audience: listOfAudiences) {
                // Firstly, check every audience, and get the audienceActivity List for this specific audience

                //List <AudienceActivity> audienceActivityList = audienceActivityRepository.getAudienceActivityByAudience(audience);
                List <AudienceActivity> audienceActivityList = audienceActivityRepository.findAllAudienceActivityByAudienceId(audience.getId());

                System.out.println("audience activity list size: "+audienceActivityList.size());

                if( audienceActivityList != null || audienceActivityList.size() != 0)
                {
                    for(AudienceActivity audienceActivity: audienceActivityList)
                    {
                        // Check every activity to see if this activity matches what we expect
                        // 1. check the activity type
                        // 2. check whether the activity happened in specific time interval
                        String currentType = audienceActivity.getEventType();

                        LocalDateTime audienceActivityCreateTime = audienceActivity.getCreatedAt();
                        LocalDateTime filterTime = null;

                        if(unit.contains("hour"))
                        {
                            filterTime = audienceActivityCreateTime.minusHours( Integer.valueOf(parsedTriggerTime[1]) );
                        }
                        else if(unit.contains("day"))
                        {
                            filterTime = audienceActivityCreateTime.minusDays( Integer.valueOf(parsedTriggerTime[1]) );
                        }
                        else if(unit.contains("month"))
                        {
                            filterTime = audienceActivityCreateTime.minusMonths( Integer.valueOf(parsedTriggerTime[1]) );
                        }
                        else if(unit.contains("year"))
                        {
                            filterTime = audienceActivityCreateTime.minusYears( Integer.valueOf(parsedTriggerTime[1]) );
                        }

                        if (currentType.equals(property)) {
                            List<Transmission> transmissionList = transmissionRepository.getTransmissionByEmail(audience.getEmail());
                            for (Transmission t: transmissionList)
                            {
                                LocalDateTime transmissionTime = t.getCreatedAt();
                                if(transmissionTime.isBefore(audienceActivityCreateTime) && transmissionTime.isAfter(filterTime))
                                {
                                    if(! audienceList1.contains(audience.getId()))
                                    {
                                        audienceList1.add(audience.getId());
                                    }

                                    //CoreModuleTask newTask = coreModuleTask;
                                    //newTask.setAudienceId1(audienceList1);
                                    //newTask.setAudienceId2(new ArrayList<>());
                                    //newTask.setCallapi(0);                      //jiaqi: important, because when calling the CMTexecutor again with this task, we don't want it to call back to our if/else controller again since this trigger has already hit
                                    //newTask.setMakenext(1);
                                    //newTask.setTaskType(0);                     //the audience must already be in our main DB, so we move a user (audience), not create one
                                    //cmtExecutor.execute(newTask);

                                    //restAudience.remove(audience);
                                    audienceActivityRepository.delete(audienceActivity);
                                }
                            }
                        }
                    }
                }
            }

        System.out.println("________________audiencelist1: ");
        for (long audienceID: audienceList1) {
            System.out.print(" "+audienceID);
        }
        System.out.println();

        System.out.println("________________audiencelist2: ");
        List<Long> audienceList2 = listOfAudienceId;
        audienceList2.removeAll(audienceList1);
        for (long audienceID: audienceList2) {
            System.out.print(" "+audienceID);
        }
        System.out.println();

        CoreModuleTask newTask = coreModuleTask;
        newTask.setAudienceId1 (audienceList1);
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

        // get the json_text from node
        Node node = NodeRepository.searchNodeByid(coreModuleTask.getNodeId());
        String json_text = node.getProperties();

        // parse the property, condition, and value
        String marker1 = "property"; String marker2 = "condition"; String marker3 = "value";
        String property = "";
        String condition = "";
        String value = "";
        int indexOfMarker1  = json_text.indexOf(marker1);
        int indexOfMarker2  = json_text.indexOf(marker2);
        int indexOfMarker3  = json_text.indexOf(marker3);
        property = json_text.substring(indexOfMarker1 + marker1.length() + 4, indexOfMarker2 - 7);
        condition = json_text.substring(indexOfMarker2 + marker2.length() + 4, indexOfMarker3 - 7);
        value = json_text.substring(indexOfMarker3 + marker3.length() + 4, json_text.length() - 4);

        System.out.println("property: "+property);
        System.out.println("condition: "+condition);
        System.out.println("value: "+value);

// -------------------------------------- address --------------------------------------
        if (property.equals("Location")) {
            if (condition.equals("Is Within")) {
                for(Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        continue;
                    }

                    // value example = "25, New York, North Tyneside, United Kingdom";

                    String miles = value.substring(0, value.indexOf(","));
                    String units = "imperial";
                    //String destination = value.substring(value.indexOf(",")+2,value.length());

                    //parse the destination
                    String[] destination = value.split("\\s*,\\s*");
                    String[] audienceAddress = audience.getAddress().split("\\s*,\\s*");

                    //String audienceAddress = audience.getAddress();
                    //String googleDistance = getDistanceGoogle(audienceAddress,destination,units);

                    //System.out.println("google distance: "+googleDistance);
                    //System.out.println(destination);

                    WorldCity dest_city = worldCityRepository.findCity(destination[1], destination[2], destination[3]);
                    WorldCity audience_city = worldCityRepository.findCity(audienceAddress[0], audienceAddress[1], audienceAddress[2]);

                    double distance = calculateDistance(dest_city.getLat(),dest_city.getLng(),audience_city.getLat(),audience_city.getLng());

                    if(distance <=Integer.valueOf(miles))
                    {
                        haveProperty.add(audience);
                    }

                    /*if(googleDistance != null) {
                        if(googleDistance.contains(",")){
                            googleDistance = googleDistance.replace(",","");
                        }
                        if(googleDistance.contains(".")){
                            googleDistance = googleDistance.substring(0,googleDistance.indexOf("."));
                        }
                        if (Integer.valueOf(googleDistance) <= Integer.valueOf(miles)){
                            haveProperty.add(audience);
                        }
                    }*/
                }
            }

            if (condition.equals("Is Not Within")) {
                for(Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        continue;
                    }

                    String miles = value.substring(0, value.indexOf(","));
                    String units = "imperial";

                    String[] destination = value.split("\\s*,\\s*");
                    String[] audienceAddress = audience.getAddress().split("\\s*,\\s*");

                    WorldCity dest_city = worldCityRepository.findCity(destination[1], destination[2], destination[3]);
                    WorldCity audience_city = worldCityRepository.findCity(audienceAddress[0], audienceAddress[1], audienceAddress[2]);

                    double distance = calculateDistance(dest_city.getLat(),dest_city.getLng(),audience_city.getLat(),audience_city.getLng());

                    if(distance > Integer.valueOf(miles))
                    {
                        haveProperty.add(audience);
                    }

                }
            }

            if (condition.equals("Is In Country")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Is Not In Country")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }

            if (condition.equals("Is In US State")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Is Not In US State")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
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
                //  1: is after date
                //  0: Date is
                // -1: is before date
                filterBirthday(value, 0, listOfAudiences, haveProperty);
            }

            if (condition.equals("Is Before Date")) {
                filterBirthday(value, -1, listOfAudiences, haveProperty);
            }

            if (condition.equals("Is After Date")) {
                filterBirthday(value, 1, listOfAudiences, haveProperty);
            }

        }

// -------------------------------------- email address --------------------------------------
        if (property.equals("Email Address")) {
            if (condition.equals("Is")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Is not")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }

            if (condition.equals("Contains")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Does Not Contain")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }
        }

// -------------------------------------- first name --------------------------------------
        if (property.equals("First Name")) {
            if (condition.equals("Is")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Is Not")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }

            if (condition.equals("Contains")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Does Not Contain")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }
        }

// -------------------------------------- last name --------------------------------------
        if (property.equals("Last Name")) {
            if (condition.equals("Is")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Is Not")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }

            if (condition.equals("Contains")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Does Not Contain")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }
        }

// -------------------------------------- full name --------------------------------------
        if (property.equals("Full Name")) {
            if (condition.equals("Is")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Is Not")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }

            if (condition.equals("Contains")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Does Not Contain")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }
        }

// -------------------------------------- phone number --------------------------------------
        if (property.equals("Phone Number")) {
            if (condition.equals("Is")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Is Not")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }

            if (condition.equals("Contains")) {
                filterWithProperty(property, condition, value, true, listOfAudiences, haveProperty);
            }

            if (condition.equals("Does Not Contain")) {
                filterWithProperty(property, condition, value, false, listOfAudiences, haveProperty);
            }
        }

// -------------------------------------- gender --------------------------------------
        if (property.equals("Gender")) {
            if (condition.equals("Is")) {
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
        int indexOfMarker1  = json_text.indexOf(marker1);
        int indexOfMarker2  = json_text.indexOf(marker2);
        int indexOfMarker3  = json_text.indexOf(marker3);
        property = json_text.substring(indexOfMarker1 + marker1.length() + 4, indexOfMarker2 - 6);
        condition = json_text.substring(indexOfMarker2 + marker2.length() + 4, indexOfMarker3 - 6);

        System.out.println("property: "+property);
        System.out.println("condition: "+condition);

        if (property.equals("Location")) {
            if (condition.equals("Is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getAddress() == null) {
                        haveProperty.add(audience);
                    }
                }
            }
        }

        if (property.equals("Email Address")) {
            if (condition.equals("Is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getEmail() == null || audience.getEmail().isEmpty()) {
                        haveProperty.add(audience);
                    }
                }
            }
        }

        if (property.equals("Birthday")) {
            if (condition.equals("Is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getBirthday() == null) {
                        haveProperty.add(audience);
                    }
                }
            }
        }

        if (property.equals("First Name")) {
            if (condition.equals("Is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() == null || audience.getFirstName().isEmpty()) {
                        haveProperty.add(audience);
                    }
                }
            }
        }

        if (property.equals("Last Name")) {
            if (condition.equals("Is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getLastName() == null || audience.getLastName().isEmpty()) {
                        haveProperty.add(audience);
                    }
                }
            }

        }

        if (property.equals("Full Name")) {
            if (condition.equals("Is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getFirstName() == null || audience.getFirstName().isEmpty()){
                        if (audience.getLastName() == null || audience.getLastName().isEmpty()) {
                            haveProperty.add(audience);
                        }
                    }
                }
            }

        }

        if (property.equals("Phone Number")) {
            if (condition.equals("Is Blank")) {
                for( Audience audience: listOfAudiences) {
                    if (audience.getPhone() == null || audience.getPhone().isEmpty()) {
                        haveProperty.add(audience);
                    }
                }
            }
        }

        if (property.equals("Gender")) {
            if (condition.equals("Is")) {
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

    private String getDistanceGoogle(String source, String destination, String units){
        // This function is used to call the Distance Matrix From Google Map API

        // This API Key is my personal key
        String API_KEY = "AIzaSyDpN84uRC0A2aKpVb9ugW86xm4g5tsNlh0";

        // This url link is the Distance Matrix from Google map api
        String url="https://maps.googleapis.com/maps/api/distancematrix/json?origins="+source+"&destinations="+destination+"&units="+units+"&key="+ API_KEY;
        HttpMethod method = HttpMethod.GET;
        String json_text = (new HttpClient()).getResponse(url,method,null);

        String distance = null;

        // parse to see whether the api function correctly
        if(!json_text.contains("ZERO_RESULTS"))
        {
            // parse the distance
            String marker1 = "text";
            String marker2 = "mi";
            int indexOfMarker1  = json_text.indexOf(marker1);
            int indexOfMarker2  = json_text.indexOf(marker2);
            distance = json_text.substring(indexOfMarker1 + marker1.length() + 5, indexOfMarker2 - 1);
        }
        return distance;
    }

    private String getDistanceMapQuest(String source, String destination, String units){
        // This function is used to call the Distance Matrix From MapQuest API

        // This API Key is my personal key
        String API_KEY = "RwnUtxgD9rEFPh2G7cbzyUAmEyfddDwK";

        String url="https://www.mapquestapi.com/directions/v2/routematrix?key="+API_KEY;
        HttpMethod method = HttpMethod.POST;

        JSONArray params = new JSONArray();
        params.put(source);
        params.put(destination);

        JSONObject json_input = new JSONObject();
        json_input.put("locations",params);

        String json_text = (new HttpClient()).getResponse(url,method,json_input.toString());

        // parse the distance
        String marker1 = "distance";
        String marker2 = "locations";
        String distance = "";
        int indexOfMarker1  = json_text.indexOf(marker1);
        int indexOfMarker2  = json_text.indexOf(marker2);
        distance = json_text.substring(indexOfMarker1 + marker1.length() + 5, indexOfMarker2 - 3);

        return distance;
    }

    private void filterBirthday(String value, int condition, List<Audience> listOfAudiences, List<Audience> haveProperty)
    {
        //  1: is after date
        //  0: Date is
        // -1: is before date
        for (Audience audience : listOfAudiences) {
            if (audience.getBirthday() == null) {
                continue;
            }
            int parseIndex = value.indexOf("/");
            int input_month = Integer.parseInt(value.substring(0, parseIndex));
            int input_day = Integer.parseInt(value.substring(parseIndex+1, value.length()));
            if(condition != 0) {
                LocalDate localDate = LocalDate.of(audience.getBirthday().getYear(), input_month, input_day);
                if (condition < 0) {
                    // -1: is before date
                    if (audience.getBirthday().compareTo(localDate) < 0) {
                        haveProperty.add(audience);
                    }
                } else {
                    //  1: is after date
                    if (audience.getBirthday().compareTo(localDate) > 0) {
                        haveProperty.add(audience);
                    }
                }
            }
            else{
                //  0: Date is
                if(audience.getBirthday().getMonthValue() == (input_month)){
                    if ((audience.getBirthday().getDayOfMonth()) == (input_day)) {
                        haveProperty.add(audience);
                    }
                }
            }
        }
    }

    private void filterWithProperty(String property, String condition, String value, boolean flag, List<Audience> listOfAudiences, List<Audience> haveProperty)
    {
        String audienceValue = null;
        for(Audience audience: listOfAudiences) {
            if (property.equals("Location")) {
                if (audience.getAddress() == null) {
                    continue;
                }else{
                    audienceValue = audience.getAddress();
                }
            } else if (property.equals("Email Address")) {
                if (audience.getEmail() == null) {
                    continue;
                }else{
                    audienceValue = audience.getEmail();
                }
            } else if (property.equals("First Name")) {
                if (audience.getFirstName() == null) {
                    continue;
                }else{
                    audienceValue = audience.getFirstName();
                }
            } else if (property.equals("Last Name"))  {
                if (audience.getFirstName() == null) {
                    continue;
                } else{
                    audienceValue = audience.getLastName();
                }
            } else if (property.equals("Full Name")) {
                if (audience.getFirstName() == null && audience.getLastName() == null) {
                    continue;
                }else{
                    audienceValue = audience.getFirstName()+" "+audience.getLastName();
                }

            } else if (property.equals("Phone Number")) {
                if (audience.getPhone() == null) {
                    continue;
                }
                else{
                    audienceValue = audience.getPhone();
                }
            }

            if(condition.contains("Contain") || condition.contains("In"))
            {
                if(flag){
                    if (audienceValue.contains(value)){
                        haveProperty.add(audience);
                    }
                }
                else {
                    if (!audienceValue.contains(value)){
                        haveProperty.add(audience);
                    }
                }
            } else if (condition.contains("Is")){
                if(flag){
                    if (audienceValue.equals(value)){
                        haveProperty.add(audience);
                    }
                }
                else {
                    if (!audienceValue.equals(value)){
                        haveProperty.add(audience);
                    }
                }
            }
        }
    }

        private static final double EARTH_RADIUS = 3958.8; // 地球半径（单位：英里 - miles）
        public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = EARTH_RADIUS * c;
            return distance;
        }

}
