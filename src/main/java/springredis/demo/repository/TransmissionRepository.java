package springredis.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import springredis.demo.entity.Transmission;

@Repository
public interface TransmissionRepository extends JpaRepository<Transmission, Long> {
}
