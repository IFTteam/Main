package springredis.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
import springredis.demo.repository.AudienceRepository;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;

import java.util.Optional;

@RestController
public class IfElseController {
    @Autowired
    IfElseTaskController ifElseTaskController;
    @Autowired
    NodeRepository NodeRepository;

    @PostMapping("/IfElse")
    public CoreModuleTask redirect(@RequestBody CoreModuleTask task) throws JsonProcessingException {
        CoreModuleTask nullTask = new CoreModuleTask();
        nullTask.setName("nullTask");

        // {'property': 'XXX', 'condition': 'YYY', 'value': 'ZZZ'}
        // {'property': 'XXX', 'condition': 'YYY', 'value': null}
        // {'repeatInterval': 'XXX', 'repeat': #, 'triggerTime': #, 'eventType': 'WWW', 'httpEntity': [{'aaa'},{'bbb'}, ... ,{'ccc'}]}

        // active_node table: node_id
        Node node = NodeRepository.searchNodeByid(task.getNodeId());
        String json_text = node.getProperties();

        System.out.println("In if/else, the node id is:" + task.getNodeId());
        System.out.println("In if/else, the json text is:" + json_text);

        if (json_text.contains("property") && json_text.contains("condition") && json_text.contains("value")) {

            String find = "value";
            String substr = "";
            int i  = json_text.indexOf(find);
            substr = json_text.substring(i + find.length() + 3, json_text.length() - 1);
            if (substr.contains("null")) {
                System.out.println("handled by ifElseProperty NoValue:" + json_text);
                return ifElseTaskController.ifElsePropertyWithoutValue(task);
            } else if (!substr.contains("null")) {
                System.out.println("handled by ifElseProperty:" + json_text);
                return ifElseTaskController.ifElseProperty(task);
            }
        } else if (json_text.contains("httpEntity") && json_text.contains("repeatInterval") && json_text.contains("triggerTime")) {
            return ifElseTaskController.filterByAudienceAction(task);
        }
        return nullTask;
    }
}
