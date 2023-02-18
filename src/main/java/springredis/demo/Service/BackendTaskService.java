package springredis.demo.Service;


import springredis.demo.entity.CoreModuleTask;

public interface BackendTaskService {
    CoreModuleTask createRelationBetweenAudienceAndTag(CoreModuleTask coreModuleTask);

    CoreModuleTask removeRelationBetweenAudienceAndTag(CoreModuleTask coreModuleTask);

    //please write a method take in coreModuleTask and return coreModuleTask and remove the tag_audience_relation if
    //the id of the tag_audience_relation audience is  in the audienceId1

}
