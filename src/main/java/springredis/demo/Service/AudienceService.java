package springredis.demo.Service;

import springredis.demo.entity.Audience;

import java.util.List;

public interface AudienceService {

    Audience save(Audience audience);

    List<Audience> saveBatch(List<Audience> audience);

}
