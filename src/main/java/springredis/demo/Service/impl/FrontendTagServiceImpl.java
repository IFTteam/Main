package springredis.demo.Service.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.Service.FrontendTagService;
import springredis.demo.entity.*;
import springredis.demo.error.AudienceNotFoundException;
import springredis.demo.error.UserNotFoundException;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.TagRepository;
import springredis.demo.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FrontendTagServiceImpl implements FrontendTagService {
    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private AudienceRepository audienceRepository;

    @Override
    public List<String> getDistinctTagByUser(Long userId) throws UserNotFoundException {
        Optional<User> user = userRepository.findById(userId);

        if (!user.isPresent()) {
            throw new UserNotFoundException("User Not Available");
        }
        User real_user = user.get();
        List<Tag> tagList = tagRepository.getTagByUser(real_user);
        List<String> tagNameResultList = new ArrayList<>();
        List<Tag> resultList = new ArrayList<>();
        for(Tag t : tagList) {
            if(!tagNameResultList.contains(t.getTag_name())) {
                tagNameResultList.add(t.getTag_name());
                resultList.add(t);
            }
        }
        List<String> tagname=new ArrayList<>();
        for(Tag element : resultList){
            tagname.add(element.getTag_name());
        }
        return tagname;
    }

    @Override
    public Tag saveTagWithUserAndJourney(long userId, Tag tag) {
        User user = userRepository.findById(userId);
        String tag_name = tag.getTag_name();
        Tag new_tag = Tag.builder()
                .tag_name(tag_name)
                .user(user)
                .build();
        return tagRepository.save(new_tag);
    }

    @Override
    public List<TagDetail> getTagAndJourneyByAudience(Long audienceId) throws AudienceNotFoundException {
        Optional<Audience> audience = audienceRepository.findById(audienceId);

        if (!audience.isPresent()) {
            throw new AudienceNotFoundException("Audience Not Available");
        }

        Audience real_audience = audience.get();
        List<TagDetail> tagList = real_audience.getTagDetails();
        return tagList;
    }


}
