package springredis.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import springredis.demo.entity.Transmission;
import springredis.demo.entity.User;
import springredis.demo.entity.WorldCity;

import java.util.List;

@Repository
public interface TransmissionRepository extends JpaRepository<Transmission, Long> {

    @Query
    List<Transmission> getTransmissionByEmail(String email);
    @Query("select t from Transmission t where t.user = user")
    List<Transmission> getTransmissionByUser(User user);

    @Query(value="SELECT * from Transmission where user_id = ?1", nativeQuery = true )
    List<Transmission> getTransmissionByUserId(long userid);
}
