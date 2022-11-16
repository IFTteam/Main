package springredis.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.entity.Audience;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Tag;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.TagRepository;

import java.util.List;
import java.util.Optional;

@Service
public class BackendTaskServiceImpl implements BackendTaskService {

    @Autowired
    private AudienceRepository audienceRepository;
    @Autowired
    private TagRepository tagRepository;

    @Override
    public CoreModuleTask createRelationBetweenAudienceAndTag(CoreModuleTask coreModuleTask) {
        String json_text = coreModuleTask.getName();
        String find = "tagId";
        String substr = "";
        int i  = json_text.indexOf(find);
        substr = json_text.substring(i + find.length() + 3, json_text.length() - 1);
        Long tagId =  Long.parseLong(substr);
        Optional<Tag> tag = tagRepository.findById(tagId);



        Tag real_tag = tag.get();


        List<Long> audienceIds = coreModuleTask.getActiveAudienceId1();
        for (Long audienceId : audienceIds) {
            Long l = audienceId;
            Optional<Audience> audience = audienceRepository.findById(l);


            Audience real_audience = audience.get();


            // Audience audience = audienceRepository.findById(audienceId).get();
            // Tag tag = tagRepository.findById(tagId).get();
            real_audience.addTags(real_tag);
            audienceRepository.save(real_audience);
        }
        CoreModuleTask newTask = coreModuleTask;
        List a = newTask.getAudienceId1();
        List b = newTask.getAudienceId2();
        newTask.setAudienceId1(a);
        newTask.setAudienceId2(b);
        //jiaqi:这里为了确保task的状态，还是要手动的set一下makenext为1来确保return之后下一个task会被task coordinator的CMTexecutor制作
        newTask.setMakenext(1);
        newTask.setTaskType(1);
        return newTask;
    }
}
