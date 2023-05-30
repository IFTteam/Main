package springredis.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.entity.WorldCity;
import springredis.demo.error.WorldCityNotExistException;
import springredis.demo.repository.WorldCityRepository;

import java.util.List;
@Service
public class WorldCityService {

    private WorldCityRepository worldCityRepository;

    @Autowired
    public WorldCityService(WorldCityRepository worldCityRepository) {
        this.worldCityRepository = worldCityRepository;
    }

    public List<WorldCity> findCityByName(String name) throws WorldCityNotExistException {
        List<WorldCity> worldCityList = worldCityRepository.findByCityContainingIgnoreCase(name);
        if (worldCityList == null) {
            throw new WorldCityNotExistException("City doesn't exist");
        }
        return worldCityList;
    }

}

