package springredis.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.Service.IfElseTaskService;
import springredis.demo.entity.CoreModuleTask;

@RestController
public class IfElseTaskController {

    @Autowired
    private IfElseTaskService ifElseTaskService;

    @GetMapping("/audienceAction/{node}/{user}/{target}/{interval}/{repeat}/{trigger}/{event}/{journey}")
    public CoreModuleTask filterByAudienceAction (CoreModuleTask coreModuleTask) throws JsonProcessingException {
        return ifElseTaskService.filterByAudienceAction(coreModuleTask);
    }

    @GetMapping("/audiences/{property}/{condition}/{value}")
    public CoreModuleTask ifElseProperty(CoreModuleTask coreModuleTask) {
        return ifElseTaskService.ifElseProperty(coreModuleTask);
    }

    @GetMapping("/audiences/{property}/{condition}")
    public CoreModuleTask ifElsePropertyWithoutValue(CoreModuleTask coreModuleTask) {
        return ifElseTaskService.ifElsePropertyWithoutValue(coreModuleTask);
    }
}