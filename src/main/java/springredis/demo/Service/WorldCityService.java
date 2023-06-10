package springredis.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.entity.WorldCity;
import springredis.demo.error.WorldCityNotExistException;
import springredis.demo.repository.WorldCityRepository;
import springredis.demo.entity.PartialRatio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
@Service
public class WorldCityService {

    private WorldCityRepository worldCityRepository;

    @Autowired
    public WorldCityService(WorldCityRepository worldCityRepository) {
        this.worldCityRepository = worldCityRepository;
    }

    public List<WorldCity> findCityByName(String targetCity, String accuracyRate) throws WorldCityNotExistException {

        if (targetCity == null) {
            throw new WorldCityNotExistException("City doesn't exist");
        }

        List<WorldCity> worldCityList = worldCityRepository.findAll();
        List<WorldCity> matchedCity = new ArrayList<>();
        int size = worldCityList.size();
        for(int i=0; i<size;i++)
        {
            WorldCity city = worldCityList.get(i);
            String matchPattern = city.getCityAscii()+", "
                    + city.getAdminName()+", "
                    + city.getCountry();
            double score = partialRatio(matchPattern,targetCity)/100;

            if(score > Double.valueOf(accuracyRate))
            {
                matchedCity.add(city);
            }
        }

        Collections.sort(matchedCity, new WorldCityComparator());

        return matchedCity;
    }

    /*
    public static int getLevenshteinDistance(String X, String Y)
    {
        int m = X.length();
        int n = Y.length();

        int[][] T = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            T[i][0] = i;
        }
        for (int j = 1; j <= n; j++) {
            T[0][j] = j;
        }

        int cost;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                cost = X.charAt(i - 1) == Y.charAt(j - 1) ? 0: 1;
                T[i][j] = Integer.min(Integer.min(T[i - 1][j] + 1, T[i][j - 1] + 1),
                        T[i - 1][j - 1] + cost);
            }
        }

        return T[m][n];
    }

    public static double findSimilarity(String x, String y) {
        if (x == null || y == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }

        double maxLength = Double.max(x.length(), y.length());
        if (maxLength > 0) {
            // optionally ignore case if needed
            return (maxLength - getLevenshteinDistance(x, y)) / maxLength;
        }
        return 1.0;
    }*/

    public static double partialRatio(String x, String y) {
        PartialRatio partialRatio = new PartialRatio();
        return partialRatio.apply(x, y);
    }
}


class WorldCityComparator implements java.util.Comparator<WorldCity> {
    @Override
    public int compare(WorldCity a, WorldCity b) {
        return (int) (a.getPopulation() - b.getPopulation());
    }
}