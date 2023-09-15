package springredis.demo.Service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.Service.AudienceAudiencelistService;
import springredis.demo.entity.AudienceAudiencelist;
import springredis.demo.repository.AudienceAudiencelistRepository;

@Service
public class AudienceAudiencelistServiceImpl implements AudienceAudiencelistService {

    @Autowired
    private AudienceAudiencelistRepository audienceAudiencelistRepository;

    @Override
    public AudienceAudiencelist save(AudienceAudiencelist audienceAudiencelist) {
        return audienceAudiencelistRepository.save(audienceAudiencelist);
    }
}
