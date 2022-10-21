package springredis.demo.controller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import springredis.demo.Service.DAO;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveAudience;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class API_Trigger_Controller {
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    DAO productService;


    //any call to any api trigger shall be posted to this url;
    //after processing the api call, return the coremoduletask that was sent in POST (in this case, don't change anything)
    @RequestMapping( value="/API_trigger",method= RequestMethod.POST)
    @ResponseBody
    public CoreModuleTask redirect(@RequestBody CoreModuleTask task){
        CoreModuleTask nulltask = new CoreModuleTask();
        nulltask.setName("nulltask");
        if(task.getName().equals("shopify_create_trigger")){
            return create_purchase_webhook(task);
        }
        else if (task.getName().equals("shopify_abandon_checkout_trigger")){
            return create_abandon_checkout_webhook(task);
        }
        return nulltask;
    }

    @RequestMapping(value="shopify_create_puchase_webhook",method=RequestMethod.POST)
    @ResponseBody
    public CoreModuleTask create_purchase_webhook(@RequestBody CoreModuleTask task) {
        User user = productService.searchUserById(task.getUserId());
        Node node = productService.searchNodeById(task.getNodeId());
        Journey journey = productService.searchJourneyById(task.getJourneyId());
        Optional<triggerType_node_relation> opstnr = productService.searchTNR(user.getId(),"purchase");
        if(!opstnr.isPresent()){
            triggerType_node_relation tnr = new triggerType_node_relation("purchase",user.getId());
            productService.addNewTNR(tnr);
        }
        triggerType_node_relation restnr = productService.searchTNR(user.getId(),"purchase").get();
        List<Node> nodes = restnr.getNodes();
        boolean found = false;
        for(Node n:nodes){
            if(n.getId()==node.getId()){
                found = true;
            }
        }
        if(!found) {
            node.setTriggertype_node_relation(restnr);
            restnr = productService.addNewTNR(restnr);                   //at this point since restnr was created before, it is an update operation to update the node field
            node = productService.addNewNode(node);
        }
        String devstore = user.getShopifydevstore();
        String token = user.getShopifyApiKey();
//        String url = "https://"+devstore+".myshopify.com/admin/api/2022-04/webhooks.json";
String url = "http://localhost:8080/show"; //for testing
        String data = "{\"webhook\":{\"topic\":\"orders/create\",\"address\":\"localhost:8080/shopify_purchase_update/"+Long.toString(user.getId())+"\",\"format\":\"json\",\"fields\":[\"id\",\"note\"]}}";
        HttpHeaders header = new HttpHeaders();
        header.set("X-Shopify-Access-Token",token);
        header.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity(data,header);
        ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST,request,String.class);       //fetches response entity from server. response is confirmation of the created webhook
        return task;
    }

    //
    @RequestMapping(value="/shopify_create_abandon_checkout_webhook",method=RequestMethod.POST)
    @ResponseBody
    public CoreModuleTask create_abandon_checkout_webhook(@RequestBody CoreModuleTask task) {
        User user = productService.searchUserById(task.getUserId());
        Node node = productService.searchNodeById(task.getNodeId());
        Journey journey = productService.searchJourneyById(task.getJourneyId());
        Optional<triggerType_node_relation> opstnr = productService.searchTNR(user.getId(),"abandon_cart");
        if(!opstnr.isPresent()){
            triggerType_node_relation tnr = new triggerType_node_relation("abandon_checkout",user.getId());
            productService.addNewTNR(tnr);
        }
        triggerType_node_relation restnr = productService.searchTNR(user.getId(),"abandon_cart").get();
        List<Node> nodes = restnr.getNodes();
        boolean found = false;
        for(Node n:nodes){
            if(n.getId()==node.getId()){
                found = true;
            }
        }
        if(!found) {
            node.setTriggertype_node_relation(restnr);
            restnr = productService.addNewTNR(restnr);                   //at this point since restnr was created before, it is an update operation to update the node field
            node = productService.addNewNode(node);
        }
        String devstore = user.getShopifydevstore();
        String token = user.getShopifyApiKey();
        String url = "https://"+devstore+".myshopify.com/admin/api/2022-04/webhooks.json";
        String data = "{\"webhook\":{\"topic\":\"checkouts/update\",\"address\":\"localhost:8080/shopify_abandon_checkout_update/"+Long.toString(user.getId())+"\",\"format\":\"json\",\"fields\":[\"id\",\"note\"]}}";
        HttpHeaders header = new HttpHeaders();
        header.set("X-Shopify-Access-Token",token);
        header.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity(data,header);
        ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST,request,String.class);       //fetches response entity from server. response is confirmation of the created webhook
        return task;
    }

    //the url now is only user-specific
    @RequestMapping(value="/shopify_purchase_update/{user}",method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<CoreModuleTask> shopify_purchasetrigger_hit(@PathVariable("user") String username, @RequestBody String jsonstr)
    {
        User user = productService.searchUserById(Long.parseLong(username));
        JSONObject order = new JSONObject(jsonstr);
        JSONObject tmp = order.getJSONObject("customer");
        Long id = tmp.getLong("id");
        String email=tmp.getString("email"),fi=tmp.getString("first_name"),li=tmp.getString("last_name");
        Audience audience = new Audience();
        audience.setEmail(email);audience.setFirstName(fi);audience.setLastName(li);
        audience.setSource("shopify");
        Long audienceid = 0L;
        Audience exsitingaudience = productService.searchAudienceByEmail(email);
        //add new audience if not included in the audience table
        if(exsitingaudience!=null){
            audienceid = productService.searchAudienceByEmail(email).getId();
        }
        else
            audienceid = productService.addNewAudience(audience).getId();
        triggerType_node_relation tnr = productService.searchTNR(user.getId(),"purchase").get();
        List<Node> nodes = tnr.getNodes();
        List<CoreModuleTask> tasks = new ArrayList<>();                 //returns all new tasks pushed onto the task queue
        //push a bunch of API_trigger-typed CMT onto task queue with name being "finished": this type of task is redirected to a "finished" controller, which only modifies the task's taskType to 1 so task executor will create (not make) audience in next node's buffer
        for(Node n:nodes){
            String url = "http://localhost:8080" + "/ReturnTask";   //replace with server domain name
            CoreModuleTask task = new CoreModuleTask();
            List<Long> newlist = new ArrayList<>();
            newlist.add(audienceid);
            task.setAudienceId1(newlist);
            task.setNodeId(n.getId());                   //we set nodeid as next node's id, since task executor should execute the next node's task, not this node
            task.setUserId(user.getId());
            task.setTaskType(1);                        //a task that is creating a new audience
            task.setName(n.getName());
            task.setType(n.getType());
            task.setSourceNodeId(n.getId());
            task.setMakenext(1);                        //When processing this task, core module will make the next task upon response from controller
            if(n.getNexts().size()!=0) task.setTargetNodeId(n.getNexts().get(0));
            HttpEntity<CoreModuleTask> request = new HttpEntity(task);
            ResponseEntity<Long> res = this.restTemplate.exchange(url,HttpMethod.POST,request,Long.class);
            tasks.add(task);
        }
        return tasks;
    }


    @RequestMapping(value="/shopify_abandon_checkout_update/{user}",method=RequestMethod.POST)
    @ResponseBody
    public List<CoreModuleTask> shopify_abandoncarttrigger_hit(@PathVariable("user") String username,  String nodeid,@RequestBody String jsonstr)
    {
        User user = productService.searchUserById(Long.parseLong(username));
        JSONObject order = new JSONObject(jsonstr);
        JSONObject tmp = order.getJSONObject("customer");
        Long id = tmp.getLong("id");
        String email=tmp.getString("email"),fi=tmp.getString("first_name"),li=tmp.getString("last_name");
        Audience audience = new Audience();
        audience.setEmail(email);audience.setFirstName(fi);audience.setLastName(li);
        audience.setSource("shopify");
        Long audienceid = 0L;
        Audience exsitingaudience = productService.searchAudienceByEmail(email);
        //add new audience if not included in the audience table
        if(exsitingaudience!=null){
            audienceid = productService.searchAudienceByEmail(email).getId();
        }
        else
            audienceid = productService.addNewAudience(audience).getId();
        triggerType_node_relation tnr = productService.searchTNR(user.getId(),"abandon_checkout").get();
        List<Node> nodes = tnr.getNodes();
        List<CoreModuleTask> tasks = new ArrayList<>();                 //returns all new tasks pushed onto the task queue
        for(Node n:nodes){
            for(Long nextid:n.getNexts()) {
                Node nextnode = productService.searchNodeById(nextid);
                String url = "http://localhost:8080" + "/ReturnTask";   //replace with server domain name
                CoreModuleTask task = new CoreModuleTask();
                List<Long> newlist = new ArrayList<>();
                newlist.add(audienceid);
                task.setAudienceId1(newlist);
                task.setNodeId(nextid);                   //we set nodeid as next node's id, since task executor should execute the next node's task, not this node
                task.setUserId(user.getId());
                task.setTaskType(1);                //a task that is creating a new audience
                task.setName(nextnode.getName());
                task.setType(nextnode.getType());
                task.setSourceNodeId(n.getId());
                task.setMakenext(1);                //When processing this task, core module will make the next task unless the task object is modified by some other module
                if(nextnode.getNexts().size()!=0) task.setTargetNodeId(nextnode.getNexts().get(0));
                HttpEntity<CoreModuleTask> request = new HttpEntity(task);
                ResponseEntity<Long> res = this.restTemplate.exchange(url,HttpMethod.POST,request,Long.class);
                tasks.add(task);
            }
        }
        return tasks;
    }

}
