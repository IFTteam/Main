package springredis.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Node;
import springredis.demo.entity.Transmission;
import springredis.demo.entity.User;
import springredis.demo.entity.Journey;
import springredis.demo.repository.*;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class IfElseController {
    @Autowired
    IfElseTaskController ifElseTaskController;
    @Autowired
    NodeRepository nodeRepository;
    @Autowired
    TransmissionRepository transmissionRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JourneyRepository journeyRepository;

    @PostMapping("/IfElse")
    public CoreModuleTask redirect(@RequestBody CoreModuleTask task) throws JsonProcessingException {
        CoreModuleTask nullTask = new CoreModuleTask();
        nullTask.setName("nullTask");

        // {'property': 'XXX', 'condition': 'YYY', 'value': 'ZZZ'}
        // {'property': 'XXX', 'condition': 'YYY', 'value': null}
        // {'repeatInterval': 'XXX', 'repeat': #, 'triggerTime': #, 'eventType': 'WWW', 'httpEntity': [{'aaa'},{'bbb'}, ... ,{'ccc'}]}

        // active_node table: node_id
        Node node = nodeRepository.searchNodeByid(task.getNodeId());
        String json_text = node.getProperties();

        System.out.println("In if/else, the node id is:" + task.getNodeId());
        System.out.println("In if/else, the json text is:" + json_text);

        // Handled by AudienceAction filter
        if (json_text.contains("Actions")) {
            return ifElseTaskController.filterByAudienceAction(task);
        }

        // Handled by Property filter
        if (json_text.contains("property") && json_text.contains("condition") && json_text.contains("value")) {
            String find = "value";
            String substr = "";
            int i  = json_text.indexOf(find);
            substr = json_text.substring(i + find.length() + 3, json_text.length() - 1);

            if (substr.contains("Blank") || substr.contains("Nothing Selected")) {
                // 通常例子：Property - Full Name, Condition - Is Blank, Value - Nothing Selected
                // 特殊例子：Property - Gender,    Condition - Is,       Value - Blank
                System.out.println("handled by ifElseProperty NoValue:" + json_text);
                return ifElseTaskController.ifElsePropertyWithoutValue(task);
            } else {
                System.out.println("handled by ifElseProperty:" + json_text);
                return ifElseTaskController.ifElseProperty(task);
            }
        } else if (json_text.contains("httpEntity") && json_text.contains("repeatInterval") && json_text.contains("triggerTime")) {
            return ifElseTaskController.filterByAudienceAction(task);
        }
        return nullTask;
    }


    @GetMapping("/getTransmission/{userId}")
    public List<String> getTransmission(@PathVariable("userId") long userId){
        // 返还和当前user对应的所有transmission
        User user = userRepository.searchUserById(userId);
        if(user != null)
        {

            // find all email title sent from this user from journey repository
            List<Node> nodeList = nodeRepository.searchNodesByCreatedByAndName(user.getId().toString(), "Send Email");

            // find the journeyId according to the FrontEndId of the nodes found
            List<Long> journeyIdList = new ArrayList<>(nodeList.size());
            Journey journey;
            for(Node n: nodeList)
            {
                journey = journeyRepository.searchJourneyByFrontEndId(n.getJourneyFrontEndId());
                journeyIdList.add(journey.getId());
            }

            // find all transmissions from this user from transmission repository
            List<Transmission> transmissionList = transmissionRepository.getTransmissionByUserId(userId);

            List<String> outputString = new ArrayList<>();

            String marker1 = "subject"; String marker2 = "content";
            String property;String emailTitle;

            for(Transmission transmission: transmissionList)
            {
                // For each transmission, check its journeyId
                for(int i = 0; i<journeyIdList.size();i++)
                {
                    // if this transmission matches any of the journeyId in list
                    if(transmission.getJourney().getId().equals(journeyIdList.get(i)))
                    {
                        // find the corresponding node and the email title saved in this node
                        property = nodeList.get(i).getProperties();
                        emailTitle = property.substring(property.indexOf(marker1) + marker1.length() + 4,
                                property.indexOf(marker2) - 6);
                        outputString.add(transmission.getId() + " " + emailTitle);
                    }
                }
            }
            return outputString;
        }
        else
        {
            System.out.println("User not found");
            return null;
        }
    }
}
