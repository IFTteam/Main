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

    @GetMapping(value = "/worldcity/{name}")
    public List<WorldCity> getWorldCity(@PathVariable String name) {
        return worldCityService.findCityByName(name);
    }
}
