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
    CoreModuleTask redirect(@RequestBody CoreModuleTask task){
        CoreModuleTask nulltask = new CoreModuleTask();
        nulltask.setName("nulltask");
        if(task.getName().equals("shopify_create_trigger")){
            return create_purchase_webhook(task);
        }
        else if (task.getName().equals("shopify_abandon_checkout_trigger")){
            return create_abandon_checkout_webhook(task);
        }
        else if (task.getName().equals("salesforce_subscription_trigger")){
            return create_salesforce_subscription_trigger(task);
        }
        return nulltask;
    }

    @RequestMapping(value="shopify_create_puchase_webhook",method=RequestMethod.POST)
    @ResponseBody
    CoreModuleTask create_purchase_webhook(@RequestBody CoreModuleTask task) {
        Audience audience = productService.searchAudienceById(task.getAudienceId());
        User user = productService.searchUserById(task.getUserId());
        Node node = productService.searchNodeById(task.getNodeId());
        Journey journey = productService.searchJourneyById(task.getJourneyId());
        //need to add
        String devstore = user.getShopifydevstore();
        String token = user.getShopifyApiKey();
//        String url = "https://"+devstore+".myshopify.com/admin/api/2022-04/webhooks.json";
String url = "http://localhost:8080/show";             //for testing purpose
        //replace local host with our server's name. Note that the designated address is specified in USER-JOURNEY-NODE style
        String data = "{\"webhook\":{\"topic\":\"orders/create\",\"address\":\"localhost:8080/Shopify/shopify_purhcase_update/"+Long.toString(user.getId())+"/"+Long.toString(journey.getId())+"/"+Long.toString(node.getId())+"\",\"format\":\"json\",\"fields\":[\"id\",\"note\"]}}";
        // create headers
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
    CoreModuleTask create_abandon_checkout_webhook(@RequestBody CoreModuleTask task) {
        Audience audience = productService.searchAudienceById(task.getAudienceId());
        User user = productService.searchUserById(task.getUserId());
        Node node = productService.searchNodeById(task.getNodeId());
        Journey journey = productService.searchJourneyById(task.getJourneyId());
        //need to add
        String devstore = user.getShopifydevstore();
        String token = user.getShopifyApiKey();
        String url = "https://"+devstore+".myshopify.com/admin/api/2022-04/webhooks.json";
        //replace local host with our server's name. Note that the designated address is specified in USER-JOURNEY-NODE style
        String data = "{\"webhook\":{\"topic\":\"checkouts/update\",\"address\":\"localhost:8080/shopify_abandon_checkout_update/"+Long.toString(user.getId())+"/"+Long.toString(journey.getId())+"/"+Long.toString(node.getId())+"\",\"format\":\"json\",\"fields\":[\"id\",\"note\"]}}";
        // create headers
        HttpHeaders header = new HttpHeaders();
        header.set("X-Shopify-Access-Token",token);
        header.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity(data,header);
        ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST,request,String.class);       //fetches response entity from server. response is confirmation of the created webhook
        return task;
    }

    @RequestMapping(value="/salesforce_create_subscription_webhook",method=RequestMethod.POST)
    @ResponseBody
    CoreModuleTask create_salesforce_subscription_trigger(@RequestBody CoreModuleTask task) {
        //create the webhook (see previous code)
        return task;
    }

    @RequestMapping(value="/shopify_purchase_update/{user}/{journey}/{node}",method=RequestMethod.POST)
    void shopify_purchasetrigger_hit(@PathVariable("user") String username,@PathVariable("journey") String journeyid, @PathVariable("node") String nodeid,@RequestBody String jsonstr)
    {
        User user = productService.searchUserById(Long.parseLong(username));
        Node node = productService.searchNodeById(Long.parseLong(nodeid));
        Journey journey = productService.searchJourneyById(Long.parseLong(journeyid));
        JSONObject order = new JSONObject(jsonstr);
        JSONObject tmp = order.getJSONObject("customer");
        Long id = tmp.getLong("id");
        String email=tmp.getString("email"),fi=tmp.getString("first_name"),li=tmp.getString("last_name");
        Audience audience = new Audience();
        audience.setEmail(email);audience.setFirstName(fi);audience.setLastName(li);
        audience.setSource("shopify");
        //add new audience if not included in the audience table
        if(!productService.searchAudienceByEmail(email).isPresent()){
            productService.addNewAudience(audience);
        }
        long audienceid = audience.getId();
        //better approach: make taskcontroller a service, directly call service instead of sending http request
        String url  = "{server_domain_name}"+"/ReturnTask";
        CoreModuleTask task = new CoreModuleTask();
        task.setNodeId(node.getId());
        task.setJourneyId(journey.getId());
        task.setAudienceId(audienceid);
        task.setUserId(user.getId());
        HttpEntity<CoreModuleTask> request = new HttpEntity(task);
        //create a task at the task controller's endpoint
        ResponseEntity<String> res = this.restTemplate.exchange(url,HttpMethod.POST,request,String.class);
    }

    @RequestMapping(value="/shopify_abandon_checkout_update/{user}/{journey}/{node}",method=RequestMethod.POST)
    void shopify_abandoncarttrigger_hit(@PathVariable("user") String username, @PathVariable("journey") String journeyid,@PathVariable("node") String nodeid,@RequestBody String jsonstr)
    {
        User user = productService.searchUserById(Long.parseLong(username));
        Node node = productService.searchNodeById(Long.parseLong(nodeid));
        Journey journey = productService.searchJourneyById(Long.parseLong(journeyid));
        JSONObject order = new JSONObject(jsonstr);
        JSONObject tmp = order.getJSONObject("customer");
        Long id = tmp.getLong("id");
        String email=tmp.getString("email"),fi=tmp.getString("first_name"),li=tmp.getString("last_name");
        Audience audience = new Audience();
        audience.setEmail(email);audience.setFirstName(fi);audience.setLastName(li);
        audience.setSource("shopify");
        //add new audience if not included in the audience table
        if(!productService.searchAudienceByEmail(email).isPresent()){
            productService.addNewAudience(audience);
        }
        long audienceid = audience.getId();
        //better approach: make taskcontroller a service, directly call service instead of sending http request
        String url  = "{server_domain_name}"+"/ReturnTask";
        CoreModuleTask task = new CoreModuleTask();
        task.setNodeId(node.getId());
        task.setJourneyId(journey.getId());
        task.setAudienceId(audienceid);
        task.setUserId(user.getId());
        HttpEntity<CoreModuleTask> request = new HttpEntity(task);
        //create a task at the task controller's endpoint
        ResponseEntity<String> res = this.restTemplate.exchange(url,HttpMethod.POST,request,String.class);
    }

    @RequestMapping(value="/salesforce_subscription_update/{user}/{node}",method=RequestMethod.POST)
    void salesforce_subscription_hit(@PathVariable("user") String username, @PathVariable("node") String nodeid)
    {

    }
}
