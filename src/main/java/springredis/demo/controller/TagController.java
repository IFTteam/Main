package springredis.demo.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
import springredis.demo.repository.NodeRepository;

@RestController
public class TagController {
    @Autowired
    private TagController_create_tag backendTaskController;

    @Autowired
    private NodeRepository nodeRepository;

    @PostMapping("/AddTag")
    public CoreModuleTask addTag(@RequestBody CoreModuleTask task) {
        CoreModuleTask nullTask = new CoreModuleTask();
        nullTask.setName("nullTask");

        Node currentNode = nodeRepository.findById(task.getNodeId()).get();
        String properties = currentNode.getProperties();
        JSONObject jsonObject = new JSONObject(properties);

        if (jsonObject.has("tag")) {
//            String find = "tagId";
//            String substr = "";
//            int i  = json_text.indexOf(find);
//            substr = json_text.substring(i + find.length() + 3, json_text.length() - 1);
//            Long tagId =  Long.parseLong(substr);
//            System.out.println(task.getName());
            return backendTaskController.createRelationBetweenAudienceAndTag(task);
        }
        return nullTask;
    }

    @PostMapping("/RemoveTag")
    public CoreModuleTask removeTag(@RequestBody CoreModuleTask task) {
        CoreModuleTask nullTask = new CoreModuleTask();
        nullTask.setName("nullTask");

        Node currentNode = nodeRepository.findById(task.getNodeId()).get();
        String properties = currentNode.getProperties();
        JSONObject jsonObject = new JSONObject(properties);

        if (jsonObject.has("tag")) {
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
