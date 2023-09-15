package springredis.demo.Service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.Service.AudienceListService;
import springredis.demo.entity.AudienceList;
import springredis.demo.repository.AudienceListRepository;

@Service
public class AudienceListServiceImpl implements AudienceListService {

    @Autowired
    private AudienceListRepository audienceListRepository;


    @Override
    public AudienceList save(AudienceList audienceList) {
       return audienceListRepository.save(audienceList);
    }
}
