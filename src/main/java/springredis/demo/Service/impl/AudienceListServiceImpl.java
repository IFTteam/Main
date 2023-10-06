package springredis.demo.Service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.Service.AudienceListService;
import springredis.demo.entity.Audience;
import springredis.demo.entity.AudienceList;
import springredis.demo.entity.request.AudienceAudiencelistVo;
import springredis.demo.repository.AudienceListRepository;
import springredis.demo.repository.AudienceRepository;

@Service
public class AudienceListServiceImpl implements AudienceListService {

    @Autowired
    private AudienceListRepository audienceListRepository;

    @Autowired
    private AudienceRepository audienceRepository;


    @Override
    public AudienceList save(AudienceList audienceList) {
       return audienceListRepository.save(audienceList);
    }

    public Audience addAudienceToAudienceList(AudienceAudiencelistVo audienceAudiencelistVo) {
        AudienceList audienceList = audienceListRepository.findById(audienceAudiencelistVo.getAudiencelistId()).orElse(null);
        Audience audience = audienceRepository.findById(audienceAudiencelistVo.getAudienceId());
        if (audienceList != null && audience != null) {
            audienceList.getAudiences().add(audience);
            audienceListRepository.save(audienceList);
        }
        return audience;
    }

}
