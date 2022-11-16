package springredis.demo.Service;


import springredis.demo.entity.CoreModuleTask;

public interface BackendTaskService {
    CoreModuleTask createRelationBetweenAudienceAndTag(CoreModuleTask coreModuleTask);
}
