package springredis.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.entity.Audience;
import springredis.demo.entity.Journey;
import springredis.demo.entity.Node;
import springredis.demo.entity.User;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.repository.*;
import springredis.demo.repository.activeRepository.ActiveAudienceRepository;
import springredis.demo.repository.activeRepository.ActiveJourneyRepository;
import springredis.demo.repository.activeRepository.ActiveNodeRepository;

import java.util.Optional;

@Service
public class DAO {
    //add more methods in future as needed
    @Autowired
    private AudienceRepository audienceRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private JourneyRepository journeyRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveAudienceRepository activeAudienceRepository;
    @Autowired
    private ActiveNodeRepository activeNodeRepository;
    @Autowired
    private ActiveJourneyRepository activeJourneyRepository;

    public DAO(){

    }

    public Audience searchAudienceById(Long id){
        return audienceRepository.searchAudienceByid(id);
    }

    public User searchUserById(Long userId) {
        return userRepository.searchUserById(userId);
    }

    public Node searchNodeById(Long Id) {
        return nodeRepository.searchNodeByid(Id);
    }

    public Journey searchJourneyById(Long Id) {
        return journeyRepository.searchJourneyById(Id);
    }

    public Optional<Audience> searchAudienceByEmail(String email) {
        return audienceRepository.searchAudienceByEmail(email);
    }

    public Optional<ActiveAudience> searchActiveAudienceByAudienceID(Long audienceID){
        return activeAudienceRepository.searchActiveAudienceByAudienceId(audienceID);
    }

    public Audience addNewAudience(Audience audience){
        return audienceRepository.save(audience);
    }

    public User addNewUser(User user){
        return userRepository.save(user);
    }

    public Node addNewNode(Node node){return nodeRepository.save(node);}

    public Journey addNewJourney(Journey journey){
        return journeyRepository.save(journey);
    }

}
