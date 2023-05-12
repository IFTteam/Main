package springredis.demo.Service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpClient {
    public String getResponse(String url, HttpMethod method, String json_input){
        // create an http client and get response
        RestTemplate template = new RestTemplate();
        ResponseEntity<String> response = null;
        if (method.matches("GET"))
        {
            response= template.getForEntity(url, String.class);
        }
        else if(method.matches("POST"))
        {
            HttpEntity<String> request = new HttpEntity<String>(json_input);
            response = template.postForEntity(url, request, String.class);
        }

        return response.getBody();
    }
}
