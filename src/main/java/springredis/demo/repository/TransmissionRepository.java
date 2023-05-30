package springredis.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import springredis.demo.entity.Transmission;

import java.util.List;

@Repository
public interface TransmissionRepository extends JpaRepository<Transmission, Long> {

    @Query
    List<Transmission> getTransmissionByEmail(String email);
}
