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
        String name = jsonObject.getString("tag");
        Journey journey=journeyRepository.searchJourneyById(coreModuleTask.getJourneyId());
        //User user=userRepository.findById(coreModuleTask.getUserId()).get();
        User user=userRepository.findById(1);

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
            tag = tagOp.get();
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
        Journey journey=journeyRepository.searchJourneyById(coreModuleTask.getJourneyId());
        User user=userRepository.findById(coreModuleTask.getUserId()).get();
        Tag tag = tagRepository.findByUserIdName(name,user).get();

        Long tagId = tag.getTagId();


        coreModuleTask.setTaskType(1);
        coreModuleTask.setTaskType(1);
        return coreModuleTask;
    }



        //please write a method take in coreModuleTask and return coreModuleTask and remove the tag_audience_relation if
    //the id of the tag_audience_relation audience is  in the audienceId1

}
