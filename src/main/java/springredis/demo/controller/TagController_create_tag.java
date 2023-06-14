package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.Service.BackendTaskService;
import springredis.demo.entity.CoreModuleTask;

@RestController
public class TagController_create_tag {

    @Autowired
    private BackendTaskService backendTaskService;

    @PostMapping("/createCoreTask")
    public CoreModuleTask createRelationBetweenAudienceAndTag(@RequestBody CoreModuleTask coreModuleTask) {
        return backendTaskService.createRelationBetweenAudienceAndTag(coreModuleTask);
    }

    @PostMapping("/removeCoreTask")
    public CoreModuleTask removeRelationBetweenAudienceAndTag(@RequestBody CoreModuleTask coreModuleTask) {
        return backendTaskService.removeRelationBetweenAudienceAndTag(coreModuleTask);
    }
}

