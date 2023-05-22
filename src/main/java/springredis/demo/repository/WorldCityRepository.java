package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.WorldCity;

@Repository
public interface WorldCityRepository extends JpaRepository<WorldCity, Long> {

    @Query(value="SELECT * from worldcities where city_ascii = ?1 and admin_name = ?2 and country = ?3", nativeQuery = true )
    WorldCity findCity(String cityName, String adminNam, String countryName);

}
