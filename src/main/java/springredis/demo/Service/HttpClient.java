package springredis.demo.Service;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpClient {
    public String getResponse(String url, HttpMethod method, MultiValueMap<String, String> params){
        // create an http client and get response
        RestTemplate template = new RestTemplate();
        ResponseEntity<String> response = template.getForEntity(url, String.class);
        return response.getBody();
    }
}
