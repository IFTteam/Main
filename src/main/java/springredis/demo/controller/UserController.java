package springredis.demo.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.repository.UserRepository;
import springredis.demo.entity.User;
import java.util.List;

@RestController
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @RequestMapping(value = "/update_user_shopifyinfo/{user}", method = RequestMethod.POST)
    @ResponseBody
    public String shopifyAbandonCartTriggerHit(@PathVariable("user") long userid, @RequestBody String jsonstr){
        int StatusCode = 200;
        String Message = "Successful update shopify infomation";
        System.out.println("JSON: " + jsonstr);
        System.out.println("User: " + userid);
        User user=userRepository.searchUserById(userid);
        JSONObject returnjson = new JSONObject();
        if (user == null){
            StatusCode = 500;
            Message = "Current user does not exist";
            returnjson.put("code",StatusCode);
            returnjson.put("message",Message);
            return returnjson.toString();
        }
        JSONObject shopify = new JSONObject(jsonstr);
        if(shopify.optString("shopify_access_token") != null) {
           user.setShopifyApiAccessToken(shopify.optString("shopify_access_token"));
        }
        if (shopify.optString("shopify_apikey") != null){
            user.setShopifyApiKey(shopify.optString("shopify_apikey"));
        }
        if (shopify.optString("shopify_devstore") != null){
            user.setShopifydevstore(shopify.optString("shopify_devstore"));
        }

        try {
            returnjson.put("code",StatusCode);
            returnjson.put("message",Message);
            userRepository.save(user);
            return returnjson.toString();
        }catch (Exception e){
            StatusCode = 500;
            Message = "Failed to update shopify infomation";
            returnjson.put("code",StatusCode);
            returnjson.put("message",Message);
            System.out.println("udpate user shopify info error:"+e);
            return returnjson.toString();
        }
    }
}
