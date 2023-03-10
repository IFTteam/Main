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
import springredis.demo.repository.AudienceRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
        //for testing purpose
        else if (task.getName().equals("Test")) {
            task.setType("finish");
            return task;
        }
        return nulltask;
    }

    @RequestMapping(value="/shopify_create_purchase_webhook",method=RequestMethod.POST)
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
        String token = user.getShopifyApiAccessToken();
        String url = "https://"+devstore+".myshopify.com/admin/api/2022-04/webhooks.json";
//String url = "http://localhost:8080/show"; //for testing
        String data = "{\"webhook\":{\"topic\":\"orders/create\",\"address\":\"https://85aa-24-85-229-120.ngrok.io/shopify_purchase_update/"+Long.toString(user.getId())+"\",\"format\":\"json\",\"fields\":[\"id\",\"note\"]}}";
        HttpHeaders header = new HttpHeaders();
        header.set("X-Shopify-Access-Token",token);
        header.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity(data, header);
        System.out.println(request.getBody());
        ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST,request,String.class);      //fetches response entity from server. response is confirmation of the created webhook
        System.out.println(response.getBody());
//        ResponseEntity<String> res = new ResponseEntity<>(data, HttpStatus.OK);
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
            triggerType_node_relation tnr = new triggerType_node_relation("abandon_cart",user.getId());
            productService.addNewTNR(tnr);
        }
        triggerType_node_relation restnr = productService.searchTNR(user.getId(),"abandon_cart").get();
        System.out.println(restnr);
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
        String token = user.getShopifyApiAccessToken();
        String url = "https://"+devstore+".myshopify.com/admin/api/2022-04/webhooks.json";
        System.out.println(url);
        String data = "{\"webhook\":{\"topic\":\"checkouts/update\",\"address\":\"https://85aa-24-85-229-120.ngrok.io/shopify_abandon_checkout_update/"+Long.toString(user.getId())+"\",\"format\":\"json\",\"fields\":[\"id\",\"note\"]}}";
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


    @RequestMapping(value = "/shopify_abandon_checkout_update/{user}", method = RequestMethod.POST)
    @ResponseBody
    public List<CoreModuleTask> shopifyAbandonCartTriggerHit(@PathVariable("user") String username, String nodeid, @RequestBody String jsonstr) {
        User user = productService.searchUserById(Long.parseLong(username));
        Long audienceId = null;
        Audience audience;
        JSONObject order = new JSONObject(jsonstr);
        String email = order.optString("email");
        String phone = order.optString("phone");
        String updateAt = order.getString("updated_at");
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(updateAt);
        LocalDateTime updateTime = offsetDateTime.toLocalDateTime();

        // The customer's contact will either be an email or a phone number
        if (!email.isEmpty()) {
            audience = productService.searchAudienceByEmail(email);
        } else {
            audience = productService.searchAudienceByPhone(phone);
        }

        if (audience != null) {
            audienceId = audience.getId();
        } else {
            audience = new Audience();
        }

        if (order.has("billing_address")) {
            JSONObject billingAddress = order.getJSONObject("billing_address");
            String firstName = billingAddress.optString("first_name");
            String lastName = billingAddress.getString("last_name");
            String address1 = billingAddress.getString("address1");
            String address2 = billingAddress.getString("address2");
            audience.setEmail(email);
            audience.setPhone(phone);
            audience.setFirstName(firstName);
            audience.setLastName(lastName);
            audience.setUpdatedAt(updateTime);
            audience.setAddress(address1 + ", " + address2);
            audience.setSource("shopify");
            productService.updateAudience(audience);
        }

        if (audienceId == null) {
            audience = productService.addNewAudience(audience);
            audience.setUpdatedAt(updateTime);
            audienceId = audience.getId();
        }

        triggerType_node_relation tnr = productService.searchTNR(user.getId(), "abandon_cart").get();
        List<Node> nodes = tnr.getNodes();
        List<CoreModuleTask> tasks = new ArrayList<>();

        for (Node node : nodes) {
            for (Long nextId : node.getNexts()) {
                Node nextNode = productService.searchNodeById(nextId);
                String url = "http://localhost:8080" + "/ReturnTask"; // Replace with server domain name
                CoreModuleTask task = new CoreModuleTask();
                List<Long> audienceIds = new ArrayList<>();
                audienceIds.add(audienceId);
                task.setAudienceId1(audienceIds);
                task.setNodeId(nextId);
                task.setUserId(user.getId());
                task.setTaskType(1);
                task.setName(nextNode.getName());
                task.setType(nextNode.getType());
                task.setSourceNodeId(node.getId());
                task.setMakenext(1);

                if (nextNode.getNexts().size() != 0) {
                    task.setTargetNodeId(nextNode.getNexts().get(0));
                }

                HttpEntity<CoreModuleTask> request = new HttpEntity<>(task);
                ResponseEntity<Long> response = restTemplate.exchange(url, HttpMethod.POST, request, Long.class);
                tasks.add(task);
            }
        }

        return tasks;
    }

    @RequestMapping(value="/salesforce-create-trigger",method=RequestMethod.POST)
    @ResponseBody
    public CoreModuleTask salesforce_create_trigger(@RequestBody CoreModuleTask task) {
        return null;
    }
}
