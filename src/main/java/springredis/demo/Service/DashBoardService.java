package springredis.demo.Service;

import java.util.HashMap;
import java.util.List;

public interface DashBoardService {

    List<List<String>> findAllJourneyById(String userid);

}
