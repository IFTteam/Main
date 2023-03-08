package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.entity.CoreModuleTask;

@RestController
public class TagController {
    @Autowired
    private TagController_create_tag backendTaskController;

    @PostMapping("/Tag")
    public CoreModuleTask redirect(@RequestBody CoreModuleTask task) {
        CoreModuleTask nullTask = new CoreModuleTask();
        nullTask.setName("nullTask");

        // {'tagId': XXX}

        String json_text = task.getName();

        if (json_text.contains("tagId")) {
//            String find = "tagId";
//            String substr = "";
//            int i  = json_text.indexOf(find);
//            substr = json_text.substring(i + find.length() + 3, json_text.length() - 1);
//            Long tagId =  Long.parseLong(substr);
//            System.out.println(task.getName());
            return backendTaskController.createRelationBetweenAudienceAndTag(task);
        }
        return task;
    }
}
