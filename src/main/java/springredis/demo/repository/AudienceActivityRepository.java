package springredis.demo.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Audience;
import springredis.demo.entity.AudienceActivity;

import java.util.List;

@Repository
public interface AudienceActivityRepository extends JpaRepository<AudienceActivity, Long> {

//    AudienceActivity findByAudience_email(String audienceEmail);
    @Query(value =
            "SELECT COUNT(*) FROM " +
                    "audience JOIN user_audience ON user_audience.audience_id = audience.id " +
                    "JOIN audience_activity ON audience_activity.audience_id = audience.id " +
                    "WHERE :audience_id = audience.id AND :event_type = audience_activity.event_type " +
                    "AND audience_activity.created_at BETWEEN :from AND :to", nativeQuery = true)
    int countByAudienceAndEventBetween(@Param("audience_id") int audienceId,
                                       @Param("event_type") String eventType,
                                       @Param("from") String from,
                                       @Param("to") String to);

    @Query(value =
            "SELECT COUNT(*) FROM " +
                    "user JOIN user_audience ON user.id = user_audience.user_id " +
                    "JOIN audience ON audience.id = user_audience.audience_id " +
                    "JOIN audience_activity ON audience.id = audience_activity.audience_id " +
                    "WHERE user.id = :user_id AND audience_activity.event_type = :event_type " +
                    "    AND audience_activity.created_at BETWEEN :from AND :to ", nativeQuery = true)
    int countByAudienceListAndEventBetween(@Param("user_id") int userId,
                                           @Param("event_type") String eventType,
                                           @Param("from") String from,
                                           @Param("to") String to);

    @Query("select s from AudienceActivity s where s.audience = ?1")
    AudienceActivity getAudienceActivityByAudience(Audience item);

    @Query("SELECT DISTINCT f.eventType FROM AudienceActivity f WHERE f.transmission_id = :transmissionId AND f.audience_email = :audienceEmail AND f.link_url = :linkUrl")
    List<String> getEventTypeByTransmissionIdAndAudienceEmailAndLinkUrl(@Param("transmissionId") Long transmissionId, @Param("audienceEmail") String audienceEmail, @Param("linkUrl") String linkUrl);
    // String getEventTypeByTransmissionIdAndAudienceEmail(@Param("transmissionId") Long transmissionId, @Param("audienceEmail") String audienceEmail);

    @Query("SELECT COUNT(DISTINCT g.eventType) FROM AudienceActivity g WHERE g.transmission_id = :transmissionId AND g.audience_email = :audienceEmail AND g.link_url = :linkUrl")
    int countDistinctEventTypeByTransmissionIdAndAudienceEmailAndLinkUrl(@Param("transmissionId") Long transmissionId, @Param("audienceEmail") String audienceEmail, @Param("linkUrl") String linkUrl);

    //@Query("select * from AudienceActivity where audience_id = ?1")
    List<AudienceActivity> findAllAudienceActivityByAudienceId(Long audienceID);

}