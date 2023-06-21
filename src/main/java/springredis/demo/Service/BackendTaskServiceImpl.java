package springredis.demo.Service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.entity.*;
import springredis.demo.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BackendTaskServiceImpl implements BackendTaskService {

    @Autowired
    private AudienceRepository audienceRepository;
    @Autowired
    private TagDetailRepository tagDetailRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CoreModuleTask createRelationBetweenAudienceAndTag(CoreModuleTask coreModuleTask) {

        Node currentNode = nodeRepository.findById(coreModuleTask.getNodeId()).get();
        String properties = currentNode.getProperties();
        JSONObject jsonObject = new JSONObject(properties);
//        System.out.println(jsonObject);
        String name = jsonObject.getString("newTag");
        if (name == null || name.isEmpty()){
            name = jsonObject.getString("tag");
        } else {
            name = jsonObject.getString("newTag");
        };

        //Get frontEndId, then find current journey
        String frontEndId = currentNode.getJourneyFrontEndId();
        Journey currentJourney = journeyRepository.searchJourneyByFrontEndId(frontEndId);

        //Find user ID by get createBy
        String userId = currentJourney.getCreatedBy();
        Long userIdLong = Long.parseLong(userId);

        Journey journey=journeyRepository.searchJourneyById(coreModuleTask.getJourneyId());
        User user=userRepository.findById(userIdLong).get();

        TagDetail real_tag = new TagDetail();

        Optional<Tag> tagOp = tagRepository.findByUserIdName(name,user);

        Tag tag;
        if (!tagOp.isPresent()){
            tag = new Tag();
            tag.setTag_name(name);
            tag.setUser(user);
            tagRepository.save(tag);
        }
        else {
            System.out.println("Tag already exists, not creating a new one.");
            tag = tagOp.get();
            System.out.println(tag);
        }

        real_tag.setJourney(journey);
        real_tag.setTagId(tag.getTagId());



        List<Long> audienceIds = coreModuleTask.getAudienceId1();

//        System.out.println("im here");
//        System.out.println(audienceIds);

        List<Long> newlist = new ArrayList<>();
        for (Long audienceId : audienceIds) {
            Long l = audienceId;
            Optional<Audience> audience = audienceRepository.findById(l);


            Audience real_audience = audience.get();


//            System.out.println(real_audience.getId());

            // Audience audience = audienceRepository.findById(audienceId).get();
            // Tag tag = tagRepository.findById(tagId).get();
            real_tag.getAudiences().add(real_audience);
            real_audience.getTagDetails().add(real_tag);

//            System.out.println(real_tag.getAudiences());

            tagDetailRepository.save(real_tag);
            audienceRepository.save(real_audience);


//            real_audience.getTags().add(real_tag);
            Long id = audienceRepository.save(real_audience).getId();

            newlist.add(id);

//            Audience curaud = audienceRepository.findById(id).get();
//            System.out.println(id);
//            System.out.println(curaud.getTags().get(0).getTag_name());

        }

//        System.out.println(newlist);

        //newly saved real audience might have change in id
        CoreModuleTask newTask = coreModuleTask;
        newTask.setAudienceId1(newlist);
        //jiaqi:这里为了确保task的状态，还是要手动的set一下makenext为1来确保return之后下一个task会被task coordinator的CMTexecutor制作
        newTask.setMakenext(1);
        newTask.setTaskType(1);
        return newTask;
    }

    @Override
    public CoreModuleTask removeRelationBetweenAudienceAndTag(CoreModuleTask coreModuleTask) {

        Node currentNode = nodeRepository.findById(coreModuleTask.getNodeId()).get();
        String properties = currentNode.getProperties();
        JSONObject jsonObject = new JSONObject(properties);
        String name = jsonObject.getString("tag");

        //Get frontEndId, then find current journey
        String frontEndId = currentNode.getJourneyFrontEndId();
        Journey currentJourney = journeyRepository.searchJourneyByFrontEndId(frontEndId);

        //Find user ID by get createBy
        String userId = currentJourney.getCreatedBy();
        Long userIdLong = Long.parseLong(userId);

        //Find the journey and user
        Journey journey=journeyRepository.searchJourneyById(coreModuleTask.getJourneyId());
        User user=userRepository.findById(userIdLong).get();
//        Tag tag = tagRepository.findByUserIdName(name,user).get();
//        Tag tag = tagRepository.findByUserIdName(name).get(0);

        //Find the tag by user and name
        Optional<Tag> tagOp = tagRepository.findByUserIdName(name, user);
        if (!tagOp.isPresent()) {
            System.out.println("Tag not found.");
            return coreModuleTask;
        }

        Tag tag = tagOp.get();
        Long TagId = tag.getTagId();

        // Find TagDetail by TagId
        List<TagDetail> tagDetailOp = tagDetailRepository.findByTagId(TagId);
        if (tagDetailOp.isEmpty()) {
            System.out.println("TagDetail not found for the id.");
            return coreModuleTask;
        }


        List<TagDetail> tagDetails = tagDetailRepository.findByTagId(TagId);

        // Get audience ids from the task
        List<Long> audienceIds = coreModuleTask.getAudienceId1();

        // Iterate through all tagDetails associated with the TagId
        for (TagDetail tagDetail : tagDetails) {

            // Iterate through the audience IDs and remove the relationship if it exists
            for (Long audienceId : audienceIds) {
                Optional<Audience> audience = audienceRepository.findById(audienceId);
                if (audience.isPresent()) {
                    Audience real_audience = audience.get();

                    // Check if the relationship exists, if yes, remove it
                    if (tagDetail.getAudiences().contains(real_audience)) {
                        // Remove the relationship
                        tagDetail.getAudiences().remove(real_audience);
                        real_audience.getTagDetails().remove(tagDetail);

                        // Save the changes
                        audienceRepository.save(real_audience);
                        tagDetailRepository.save(tagDetail);

                        System.out.println("Tag-audience relation removed.");
                    } else {
                        System.out.println("Tag-audience relation not found for audience id: " + audienceId);
                    }
                } else {
                    System.out.println("Audience not found for id: " + audienceId);
                }
            }
        }



        coreModuleTask.setMakenext(1);
        coreModuleTask.setTaskType(1);
        return coreModuleTask;
    }



        //please write a method take in coreModuleTask and return coreModuleTask and remove the tag_audience_relation if
    //the id of the tag_audience_relation audience is  in the audienceId1

}
