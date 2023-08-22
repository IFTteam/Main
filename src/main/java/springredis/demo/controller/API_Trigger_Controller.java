package springredis.demo.controller;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import springredis.demo.Service.DAO;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.repository.JourneyRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class API_Trigger_Controller {
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    DAO productService;
    @Autowired
    private JourneyRepository journeyRepository;

    //any call to any api trigger shall be posted to this url;
    //after processing the api call, return the coremoduletask that was sent in POST (in this case, don't change anything)
    @RequestMapping( value="/API_trigger",method= RequestMethod.POST)
    @ResponseBody
    public CoreModuleTask redirect(@RequestBody CoreModuleTask task){
        switch (task.getName()) {
            case "Place a Purchase":
                return create_purchase_webhook(task);
            case "Abandon Checkout":
                return create_abandon_checkout_webhook(task);
            default:
                task.setMakenext(0);
                return task;
        }
    }

    private boolean checkIfWebhookExists(CoreModuleTask task, String type) {
        User user = productService.searchUserById(task.getUserId());
        String devstore = user.getShopifydevstore();
        String token = user.getShopifyApiAccessToken();
        String api_key = user.getShopifyApiKey();
        String url = "https://" + api_key + ":" + token + "@" + devstore + ".myshopify.com/admin/api/2023-01/webhooks.json";
        System.out.println(url);
        HttpHeaders header = new HttpHeaders();
        header.set("X-Shopify-Access-Token",token);
        header.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity("{}", header);
        ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        JSONObject responseJson = new JSONObject(response.getBody());
        JSONArray webhooksArray = responseJson.getJSONArray("webhooks");

        for (int i = 0; i < webhooksArray.length(); i++) {
            JSONObject webhookJson = webhooksArray.getJSONObject(i);
            String topic = webhookJson.getString("topic");
            if (type.equals(topic)) {
                return true;
            }
        }
        return false;
    }

    @RequestMapping(value="/shopify_create_purchase_webhook",method=RequestMethod.POST)
    @ResponseBody
    public CoreModuleTask create_purchase_webhook(@RequestBody CoreModuleTask task) {
        User user = productService.searchUserById(task.getUserId());
        Node node = productService.searchNodeById(task.getNodeId());
        System.out.println("node: " + node.toString());
        Optional<triggerType_node_relation> opstnr = productService.searchTNR(user.getId(),"purchase");
        if(!opstnr.isPresent()){
            triggerType_node_relation tnr = new triggerType_node_relation("purchase",user.getId());
            productService.setNewTNR(tnr);
        }
        triggerType_node_relation restnr = productService.searchTNR(user.getId(),"purchase").get();
        System.out.println("========= TNR is :" + restnr);

        // removing all the old trigger nodes
        List<Node> nodes = restnr.getNodes();
        for(Node n : nodes) {
            System.out.println("Old node: " + n.getId());
            n.setTriggertype_node_relation(null);
        }
        // setting the new trigger node to relate to TNR
        System.out.println("========= Node " + node.getId() + " is NOT found in TNR, adding it into TNR");
        node.setTriggertype_node_relation(restnr);          //setting node's tnr node id
        restnr.setNode(node);                               //setting node to tnr's nodes list
        productService.setNewTNR(restnr);                   //update restnr

        restnr = productService.searchTNR(user.getId(),"purchase").get();
        System.out.println("========= TNR after update is :" + restnr);
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // webhook exist already
        if(checkIfWebhookExists(task, "orders/create")) {
            System.out.println("========= Webhook already exists, returning...");
        }
        // webhook doesn't exist
        else {
            System.out.println("========= Webhook does not exists, creating...");
            String devstore = user.getShopifydevstore();
            String token = user.getShopifyApiAccessToken();
            String url = "https://" + devstore + ".myshopify.com/admin/api/2023-04/webhooks.json";
            //String url = "http://localhost:8080/show"; //for testing
            String data = "{\"webhook\":{\"topic\":\"orders/create\",\"address\":\"https://5112-131-179-156-9.ngrok-free.app/shopify_purchase_update/" + user.getId() + "\",\"format\":\"json\",\"fields\":[\"id\", \"email\", \"created_at\", \"updated_at\", \"total_price\", \"customer\", \"line_items\"]}}";
            HttpHeaders header = new HttpHeaders();
            header.set("X-Shopify-Access-Token", token);
            header.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity(data, header);
            System.out.println(url);
            System.out.println("Request: " + request.getBody());
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, request, String.class);      //fetches response entity from server. response is confirmation of the created webhook
            System.out.println("Response: " + response.getBody());
        }
        // don't let the next node get executed
        task.setMakenext(0);
        return task;
    }

    //
    @RequestMapping(value="/shopify_create_abandon_checkout_webhook",method=RequestMethod.POST)
    @ResponseBody
    public CoreModuleTask create_abandon_checkout_webhook(@RequestBody CoreModuleTask task) {
        User user = productService.searchUserById(task.getUserId());
        Node node = productService.searchNodeById(task.getNodeId());

        // retrieve or create TNR from database for given userId and type: "abandon_cart"
        Optional<triggerType_node_relation> opstnr = productService.searchTNR(user.getId(),"abandon_cart");
        if(!opstnr.isPresent()){
            System.out.println("========= TNR not exists for user" + user.getId() + "'s abandon_cart");
            triggerType_node_relation tnr = new triggerType_node_relation("abandon_cart",user.getId());
            productService.setNewTNR(tnr);
        }
        triggerType_node_relation restnr = productService.searchTNR(user.getId(),"abandon_cart").get();
        System.out.println("========= TNR is :" + restnr);

        // removing all the old trigger nodes
        List<Node> nodes = restnr.getNodes();
        for(Node n : nodes) {
            System.out.println("Old node: " + n.getId());
            n.setTriggertype_node_relation(null);
        }
        // setting the new trigger node to relate to TNR
        System.out.println("========= Node " + node.getId() + " is NOT found in TNR, adding it into TNR");
        node.setTriggertype_node_relation(restnr);          //setting node's tnr node id
        restnr.setNode(node);                               //setting node to tnr's nodes list
        productService.setNewTNR(restnr);                   //update restnr

        restnr = productService.searchTNR(user.getId(),"abandon_cart").get();
        System.out.println("========= TNR after update is :" + restnr);
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // webhook exist already
        if(checkIfWebhookExists(task, "checkouts/create")) {
            System.out.println("========= Webhook already exists, returning...");
        }
        // webhook doesn't exist
        else {
            System.out.println("========= Webhook does not exists, creating...");
            String devstore = user.getShopifydevstore();
            String token = user.getShopifyApiAccessToken();
            String url = "https://"+devstore+".myshopify.com/admin/api/2023-04/webhooks.json";
            System.out.println(url);
            String data = "{\"webhook\":{\"topic\":\"checkouts/create\",\"address\":\"https://5112-131-179-156-9.ngrok-free.app/shopify_abandon_checkout_update/"+Long.toString(user.getId())+"\",\"format\":\"json\",\"fields\":[\"id\",\"abandoned_checkout_url\"]}}";
            HttpHeaders header = new HttpHeaders();
            header.set("X-Shopify-Access-Token",token);
            header.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity(data,header);
            System.out.println("Request: " + request.getBody());
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST,request,String.class);       //fetches response entity from server. response is confirmation of the created webhook
            System.out.println("Response: " + response.getBody());
        }
        // don't let the next node get executed yet
        task.setMakenext(0);
        return task;
    }

    //the url now is only user-specific
    @RequestMapping(value="/shopify_purchase_update/{user}",method=RequestMethod.POST,produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<CoreModuleTask> shopifyPurchaseTriggerHit(@PathVariable("user") String username, @RequestBody String jsonstr)
    {
        System.out.println("Purchase Trigger Hit JSON: " + jsonstr);
        User user = productService.searchUserById(Long.parseLong(username));
        Audience audience = null;
        JSONObject order = new JSONObject(jsonstr);

        JSONObject customer = order.getJSONObject("customer");
        String firstName = customer.optString("first_name");
        String lastName = customer.getString("last_name");
        String updateAt = order.getString("updated_at");
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(updateAt);
        LocalDateTime updateTime = offsetDateTime.toLocalDateTime();

        // Email and phone, one of them will be null.
        String email = customer.optString("email");

        JSONObject defaultAddress = customer.getJSONObject("default_address");
        String address1 = defaultAddress.getString("address1");
        String address2 = defaultAddress.optString("address2");
////
        Long audienceId = null;
        // The customer's contact will either be an email or a phone number
        if (!email.isEmpty()) {
            audience = productService.searchAudienceByEmail(email);
        } else {
            return null;
        }

        if (audience != null) {
            audienceId = audience.getId();
        } else {
            audience = new Audience();
        }

        audience.setFirstName(firstName);
        audience.setLastName(lastName);
        audience.setUpdatedAt(updateTime);
        audience.setCreatedAt(updateTime);////
        audience.setEmail(email);
        audience.setUser(user);

        if (address2.isEmpty()) {
            audience.setAddress(address1);
        } else {
            audience.setAddress(address1 + ", " + address2);
        }
        audience.setSource("shopify");

        if (audienceId == null) {
            audience.setCreatedAt(updateTime);
            audience = productService.addNewAudience(audience);
            audienceId = audience.getId();
        }
        productService.updateAudience(audience);

        triggerType_node_relation tnr = productService.searchTNR(user.getId(),"purchase").get();
        List<Node> nodes = tnr.getNodes();
        List<CoreModuleTask> tasks = new ArrayList<>();                 //returns all new tasks pushed onto the task queue
        //push a bunch of API_trigger-typed CMT onto task queue with name being "finished": this type of task is redirected to a "finished" controller, which only modifies the task's taskType to 1 so task executor will create (not make) audience in next node's buffer
        for(Node n:nodes){
            String url = "http://localhost:8080" + "/ReturnTask";   //replace with server domain name
            CoreModuleTask task = new CoreModuleTask();
            List<Long> newlist = new ArrayList<>();
            newlist.add(audienceId);
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
        System.out.println("Abandon Trigger Hit JSON: " + jsonstr);
        System.out.println("User: " + username);
        System.out.println("NodeId: " + nodeid);
        User user = productService.searchUserById(Long.parseLong(username));
        Long audienceId = null;
        Audience audience;
        JSONObject order = new JSONObject(jsonstr);
        // if checkout is already completed
        if(order.optString("completed_at") != null) {
            System.out.println(order.optString("completed_at"));
            return null;
        }
        String email = order.optString("email");
        String updateAt = order.getString("updated_at");
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(updateAt);
        LocalDateTime updateTime = offsetDateTime.toLocalDateTime();

        // only search for audience if customer has email
        if (!email.isEmpty()) {
            audience = productService.searchAudienceByEmail(email);
        } // if not, simply returns
        else {
            System.out.println("================ No email or phone information provided");
            return null;
        }
        // get the audienceId if found audience in the repo
        if (audience != null) {
            audienceId = audience.getId();
            System.out.println("================ Customer exists in audience table with audienceId: " + audienceId);
        } // create a new audience if no matching audience is found with that email
        else {
            System.out.println("================ Customer does not exist in audience table");
            audience = new Audience();
            audience.setEmail(email);
            audience.setCreatedAt(updateTime);
            audience.setCreatedBy("System");
            audience.setDate_added(updateTime.toLocalDate());
            System.out.println(updateTime.toLocalDate());
        }

        if (order.has("billing_address")) {
            JSONObject billingAddress = order.getJSONObject("billing_address");
            String firstName = billingAddress.optString("first_name");
            String lastName = billingAddress.getString("last_name");
            String address1 = billingAddress.getString("address1");
            String address2 = billingAddress.optString("address2");
            audience.setFirstName(firstName);
            audience.setLastName(lastName);
            audience.setUpdatedAt(updateTime);
            audience.setUpdatedBy("System");
            audience.setUser(user);

            if (address2.isEmpty()) {
                audience.setAddress(address1);
            } else {
                audience.setAddress(address1 + ", " + address2);
            }
            audience.setSource("shopify");
        }

        // create/update audience for customer
        productService.updateAudience(audience);
        audienceId = audience.getId();
        // create active audience for customer
        ActiveAudience activeAudience = new ActiveAudience(audienceId);
        productService.addNewActiveAudience(activeAudience);
        Long activeAudienceId = activeAudience.getId();

        // get nodes to be executed through TNR
        triggerType_node_relation tnr = productService.searchTNR(user.getId(), "abandon_cart").get();
        List<Node> nodes = tnr.getNodes();
        List<CoreModuleTask> tasks = new ArrayList<>();

        for (Node node : nodes) {
            System.out.println("++++++++ Node id in tnr: " + node.getId() + " +++++++++");
            for (Long nextId : node.getNexts()) {
                System.out.println("Next node id: " + nextId);
                Node nextNode = productService.searchNodeById(nextId);
                String url = "http://localhost:8080" + "/ReturnTask"; // Replace with server domain name
                CoreModuleTask task = new CoreModuleTask();
                Journey journey = journeyRepository.searchJourneyByFrontEndId(node.getJourneyFrontEndId());
                task.setJourneyId(journey.getId());

                // set audienceIds and activeAudienceIds
                List<Long> audienceIds = new ArrayList<>();
                List<Long> activeAudienceIds = new ArrayList<>();
                audienceIds.add(audienceId);
                activeAudienceIds.add(activeAudienceId);
                task.setActiveAudienceId1(activeAudienceIds);
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
