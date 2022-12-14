package springredis.demo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.ElementCollection;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@Data
public class NodeJsonModel {
    String id;
    String componentType;
    String type;
    String name;
    Property properties; // I'll need to figure out how to deal with objects with no value.
    String createdAt; // Input's type is JavaScript Date. Store as string then convert to Java's type later.
    String updatedAt;
    String createdBy;
    String updatedBy;
    String status;
    String journeyFrontEndId;
    Branch branches;
    List<Long> nexts = new ArrayList<>();
    List<Long> lasts = new ArrayList<>();
    public NodeJsonModel(){

    }
    public NodeJsonModel(String id,
                         String componentType,
                         String type,
                         Property properties,
                         String createdAt,
                         String updatedAt,
                         String createdBy,
                         String updatedBy,
                         Branch branches,
                         String status,
                         String journeyFrontEndId) {
        this.id = id;
        this.componentType = componentType;
        this.type = type;
        this.properties = properties;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.branches = branches;
        this.status = status;
        this.journeyFrontEndId = journeyFrontEndId;
    }
    @Data
    public class Property {
        String Run;

        @JsonProperty("Select List")
        String SelectList;
    }
    @Data
    public class Branch {
        NodeJsonModel[] True;
        NodeJsonModel[] False;
    }
}