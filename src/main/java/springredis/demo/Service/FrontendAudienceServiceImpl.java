package springredis.demo.Service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.entity.Audience;
import springredis.demo.entity.Tag;
import springredis.demo.entity.User;
import springredis.demo.error.AudienceNotFoundException;
import springredis.demo.error.TagNotFoundException;
import springredis.demo.error.UserNotFoundException;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.TagRepository;
import springredis.demo.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class FrontendAudienceServiceImpl implements FrontendAudienceService {
    @Autowired
    private AudienceRepository audienceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TagRepository tagRepository;

    @Override
    public List<Audience> getAudienceAndTagByUser(Long userId) throws UserNotFoundException {
        Optional<User> user = userRepository.findById(userId);

        if (!user.isPresent()) {
            throw new UserNotFoundException("User Not Available");
        }
        User real_user = user.get();
        // User user = userRepository.findById(userId).get();

        List<Audience> audienceList = audienceRepository.getAudienceByUser(real_user);
        return audienceList;
    }
    //   @Override
//    public Audience createRelationBetweenAudienceAndTag(Long audienceId, Long tagId) throws AudienceNotFoundException, TagNotFoundException {
//        Optional<Audience> audience = audienceRepository.findById(audienceId);
//
//        if (!audience.isPresent()) {
//            throw new AudienceNotFoundException("Audience Not Available");
//        }
//
//        Audience real_audience = audience.get();
//
//        Optional<Tag> tag = tagRepository.findById(tagId);
//
//        if (!tag.isPresent()) {
//            throw new TagNotFoundException("Tag Not Available");
//        }
//
//        Tag real_tag = tag.get();
//
//        // Audience audience = audienceRepository.findById(audienceId).get();
//        // Tag tag = tagRepository.findById(tagId).get();
//        real_audience.addTags();
//        return audienceRepository.save(real_audience);
//    }


}
