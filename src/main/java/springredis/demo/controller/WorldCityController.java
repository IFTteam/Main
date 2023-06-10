package springredis.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springredis.demo.Service.WorldCityService;
import springredis.demo.entity.WorldCity;

import java.util.List;

@RestController
public class WorldCityController {
    private WorldCityService worldCityService;

    @Autowired
    public WorldCityController(WorldCityService worldCityService) {
        this.worldCityService = worldCityService;
    }

    @GetMapping(value = "/worldcity/{name}/{accuracyRate}")
    public List<String> getWorldCity(@PathVariable String name, @PathVariable String accuracyRate) {
        // accuracy Rate指的是匹配精度，越高，则对相似的标准越严格
        // 我自己测试时，0.85左右即可正常工作
        return worldCityService.searchForSimilarCity(name, accuracyRate);
    }
}
