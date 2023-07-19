package springredis.demo.Service.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.Service.FrontendAudienceService;
import springredis.demo.entity.Audience;
import springredis.demo.entity.User;
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


}
