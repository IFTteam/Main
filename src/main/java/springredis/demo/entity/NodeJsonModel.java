package springredis.demo.entity;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.ElementCollection;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@Data
public class NodeJsonModel {
    Long id;
    String componentType;
    String type;
    String name;
    Property properties; // I'll need to figure out how to deal with objects with no value.
    String createdAt; // Input's type is JavaScript Date. Store as string then convert to Java's type later.
    String updatedAt;
    String createdBy;
    String updatedBy;
    Branch branches;
    List<Long> nexts = new ArrayList<>();
    List<Long> lasts = new ArrayList<>();

    public NodeJsonModel(Long id, String componentType, String type, Property properties, String createdAt, String updatedAt, String createdBy, String updatedBy, Branch branches) {
        this.id = id;
        this.componentType = componentType;
        this.type = type;
        this.properties = properties;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.branches = branches;
    }
    @Data
    public class Property {
        String dummyVariable;
    }
    @Data
    public class Branch {
        NodeJsonModel[] True;
        NodeJsonModel[] False;
    }
}