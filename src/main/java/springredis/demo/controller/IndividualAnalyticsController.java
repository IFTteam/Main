package springredis.demo.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import springredis.demo.entity.response.AnalyticsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j // logger
@RestController
@RequestMapping(path = "/analytics/individual")
public class IndividualAnalyticsController {

    @Value("${SPARKPOST_API_KEY}")
    private String SPARKPOST_API_KEY = "0cce3a0b7ea7d58f4ca86f19425774b37d79cf6c";
    private String startTime = "2019-01-01T00:00";

    @RequestMapping(value={"/email/{email}"}, method = GET)
    @ResponseBody
    public List<AnalyticsResponse> getIndividualAnalyticsEmail(@PathVariable("email") String audienceEmail) {
        String url = "https://api.sparkpost.com/api/v1/events/message?" +
                "from=" + startTime +
                "&recipients=" + audienceEmail;
        return getIndividualAnalytics(url);
    }

    @RequestMapping(value={"/transmission/{transmissionID}"}, method = GET)
    @ResponseBody
    public List<AnalyticsResponse> getIndividualAnalyticsTransmissionID(@PathVariable("transmissionID") String transmissionID) {
        String url = "https://api.sparkpost.com/api/v1/events/message?" +
                "from=" + startTime +
                "&transmissions=" + transmissionID;
        return getIndividualAnalytics(url);
    }

    public List<AnalyticsResponse> getIndividualAnalytics(String url)
    {
        WebClient client = WebClient.create(url);
        HttpHeaders headers = new HttpHeaders();

        headers.setBasicAuth(SPARKPOST_API_KEY);

        ResponseEntity<String> Response = client.get()
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .toEntity(String.class)
                .block();

        assert Response != null;
        String responseBody = Response.getBody();
        HttpStatus statusCode = Response.getStatusCode();

        // Process the response body
        JSONObject json = new JSONObject(responseBody);
        JSONArray resultList = json.getJSONArray("results");

        Type listOfMyClassObject = new TypeToken<ArrayList<AnalyticsResponse>>() {
        }.getType();

        Gson gson = new Gson();
        List<AnalyticsResponse> AnalyticsResponseList = gson.fromJson(resultList.toString(), listOfMyClassObject);


        return AnalyticsResponseList;
    }
}
