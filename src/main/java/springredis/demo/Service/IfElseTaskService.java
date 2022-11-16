package springredis.demo.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import springredis.demo.entity.CoreModuleTask;

public interface IfElseTaskService {
    CoreModuleTask filterByAudienceAction(CoreModuleTask coreModuleTask) throws JsonProcessingException;

    CoreModuleTask ifElseProperty(CoreModuleTask coreModuleTask);

    CoreModuleTask ifElsePropertyWithoutValue(CoreModuleTask coreModuleTask);
}
