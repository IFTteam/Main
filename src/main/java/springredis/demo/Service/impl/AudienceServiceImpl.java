package springredis.demo.Service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.Service.AudienceService;
import springredis.demo.entity.Audience;
import springredis.demo.repository.AudienceRepository;

import java.util.List;

@Service
public class AudienceServiceImpl implements AudienceService {

    @Autowired
    private AudienceRepository audienceRepository;

    @Override
    public Audience save(Audience audience) {
        return audienceRepository.save(audience);
    }

    @Override
    public List<Audience> saveBatch(List<Audience> audience) {
        return audienceRepository.saveAll(audience);
    }
}
