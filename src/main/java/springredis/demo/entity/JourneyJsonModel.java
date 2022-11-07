package springredis.demo.entity;

import lombok.Data;

@Data
public class JourneyJsonModel {
    JourneyProperty properties;
    NodeJsonModel[] sequence;


    @Data
    public class JourneyProperty {
        String journeyName;
        String createdAt; // Input's type is JavaScript Date. Store as string then convert to Java's type later.
        String updatedAt;
        String createdBy;
        String updatedBy;
        String journeyId;
        String thumbNailURL;
        int status;
        String stage;
    }
}