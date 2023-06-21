package springredis.demo.entity;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.persistence.Entity;

@Entity
@Data
@Table(name="worldcities")
public class WorldCity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private String city;
    private String cityAscii;
    private double lat;
    private double lng;

    private String country;
    private String iso2;
    private String iso3;
    private  String  adminName;
    private  String capital;
    private long  population;

    private WorldCity(Builder builder) {
        this.id = builder.id;
        this.city = builder.city;
        this.cityAscii = builder.cityAscii;
        this.lat = builder.lat;
        this.lng = builder.lng;
        this.country = builder.country;
        this.adminName = builder.adminName;

    }

    public WorldCity() {

    }
    // Getters
    public Long getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public String getCityAscii() {
        return cityAscii;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getCountry() {
        return country;
    }

    public String getAdminName() {
        return adminName;
    }

    // Builder patter - Class
    public static class Builder {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("city")
        private String city;

        @JsonProperty("city_ascii")
        private String cityAscii;

        @JsonProperty("lat")
        private double lat;

        @JsonProperty("lng")
        private double lng;

        @JsonProperty("country")
        private String country;

        @JsonProperty("admin_name")
        private String adminName;

    }
}

