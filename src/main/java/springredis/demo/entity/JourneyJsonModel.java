package springredis.demo.entity;

import lombok.Data;

@Data
public class JourneyJsonModel {
    JourneyProperty properties;
    NodeJsonModel[] sequence;

    @Data
    public class JourneyProperty {
        String journeyName;
    }
}