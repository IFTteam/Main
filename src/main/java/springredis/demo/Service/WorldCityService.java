package springredis.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.entity.WorldCity;
import springredis.demo.error.WorldCityNotExistException;
import springredis.demo.repository.WorldCityRepository;
import springredis.demo.entity.PartialRatio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class WorldCityService {

    private WorldCityRepository worldCityRepository;
    private List<WorldCity> worldCityList = new ArrayList<WorldCity>();

    @Autowired
    public WorldCityService(WorldCityRepository worldCityRepository) {
        this.worldCityRepository = worldCityRepository;
    }

    public List<String> searchForSimilarCity(String targetCity, String accuracyRate) throws WorldCityNotExistException {
        // accuracy Rate指的是匹配精度，越高，则对相似的标准越严格
        // 我自己测试时，0.85左右即可正常工作

        // Check to see if the input is null
        if (targetCity == null) {
            throw new WorldCityNotExistException("City doesn't exist");
        }

        // Initialize the worldCityList
        if(worldCityList.isEmpty())
        {
            worldCityList = worldCityRepository.findAll();
        }

        List<WorldCity> matchedCity = new ArrayList<>();
        int size = worldCityList.size();
        int count = 0;
        for(int i=0; i<size; i++)
        {
            // For each city in worldcitylist, set up its string address in the format:
            // city, Admin, Country
            WorldCity city = worldCityList.get(i);
            String matchPattern = city.getCityAscii()+", "
                    + city.getAdminName()+", "
                    + city.getCountry();

            // Calculate the partialRatio score (0~100) and cast it into 0~1
            double score = partialRatio(matchPattern.toLowerCase(), targetCity.toLowerCase())/100;

            if(score > Double.valueOf(accuracyRate))
            {
                matchedCity.add(city);
                count++;
                if(count >= 200)
                {
                    break;
                }
            }
        }

        // Sort the mactchedCity list by decreasing population
        Collections.sort(matchedCity, new WorldCityComparator());

        // Cast the sorted matchedCity into String for output
        int matchedCitySize = matchedCity.size();
        List<String> matchedCityStringList = new ArrayList<String>(matchedCitySize);
        for(int i = 0; i< matchedCitySize; i++)
        {
            WorldCity city = matchedCity.get(i);
            String cityString =  city.getCityAscii()+", "+ city.getAdminName()+", " + city.getCountry();
            if(!matchedCityStringList.contains(cityString))
            {
                matchedCityStringList.add(cityString);
            }
        }

        return matchedCityStringList;
    }

    public static double partialRatio(String x, String y) {
        PartialRatio partialRatio = new PartialRatio();
        return partialRatio.apply(x, y);
    }
}

class WorldCityComparator implements java.util.Comparator<WorldCity> {
    @Override
    public int compare(WorldCity a, WorldCity b) {
        return (int) (b.getPopulation() - a.getPopulation());
    }
}