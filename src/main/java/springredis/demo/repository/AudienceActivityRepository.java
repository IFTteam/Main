package springredis.demo.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.Audience;
import springredis.demo.entity.AudienceActivity;

@Repository
public interface AudienceActivityRepository extends JpaRepository<AudienceActivity, Long> {
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

}
