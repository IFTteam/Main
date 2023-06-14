package springredis.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import springredis.demo.entity.*;
import springredis.demo.entity.activeEntity.ActiveAudience;
import springredis.demo.entity.activeEntity.ActiveNode;
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
    @Autowired
    private TNR_Repository tnr_repository;

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

    public Audience searchAudienceByEmail(String email) {
        return audienceRepository.searchAudienceByEmail(email);
    }
    public Audience searchAudienceByPhone(String phone) {
        return audienceRepository.searchAudienceByPhone(phone);
    }

    public ActiveNode searchActiveNodeById(Long id){return activeNodeRepository.findByActiveNodeId(id);}

    public ActiveAudience searchActiveAudienceByAudienceID(Long audienceID){
        return activeAudienceRepository.findByDBId(audienceID);
    }

    public Optional<triggerType_node_relation> searchTNR(Long uid,String type){return tnr_repository.searchTNR(uid,type);}

    public Audience addNewAudience(Audience audience){
        return audienceRepository.save(audience);
    }

    public User addNewUser(User user){
        return userRepository.save(user);
    }

    public Node addNewNode(Node node){return nodeRepository.save(node);}

    public ActiveNode addNewActiveNode(ActiveNode node){return activeNodeRepository.save(node);}

    public ActiveAudience addNewActiveAudience(ActiveAudience aud){return activeAudienceRepository.save(aud);}



    public Journey addNewJourney(Journey journey){
        return journeyRepository.save(journey);
    }

    public triggerType_node_relation setNewTNR(triggerType_node_relation tnr){
        return tnr_repository.save(tnr);
    }

    public void updateAudience(Audience audience) { audienceRepository.save(audience); }
}
