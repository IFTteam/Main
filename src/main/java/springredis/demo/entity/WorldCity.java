package springredis.demo.entity;

import javax.persistence.*;
import lombok.Data;
import javax.persistence.Entity;

@Entity
@Data
@Table
public class WorldCity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private String city;
    private String city_ascii;
    private double lat;
    private double lng;

    private String country;
    private String iso2;
    private String iso3;
    private  String  admin_name;
    private  String capital;
    private long  population;
}
