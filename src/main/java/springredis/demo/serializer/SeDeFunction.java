package springredis.demo.serializer;

import com.google.gson.*;
import springredis.demo.entity.Node;
import springredis.demo.entity.JourneyJsonModel;
import springredis.demo.entity.NodeJsonModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SeDeFunction {
    //传进来的是node list，把一个一个node拿出来序列化，然后加入String，返回String
    GsonBuilder gsonBuilder = new GsonBuilder();

    // used to parse the journey JSON from frontend
    public JourneyJsonModel deserializeJounrey(String journeyJson){
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new springredis.demo.serializer.LocalDateTimeSerializer());
        Gson gson = gsonBuilder.setPrettyPrinting().create();
        JourneyJsonModel journeyObject = gson.fromJson(journeyJson, JourneyJsonModel.class);
        return journeyObject;
    }

    public NodeJsonModel JsonToNodeJsonModel(String nodeJson) {
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new springredis.demo.serializer.LocalDateTimeSerializer());
        Gson gson = gsonBuilder.setPrettyPrinting().create();
        NodeJsonModel nodeObject = gson.fromJson(nodeJson, NodeJsonModel.class);
        return nodeObject;
    }

    public String serializing(ArrayList<Node> nodeArray){
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new springredis.demo.serializer.LocalDateTimeSerializer());
        Gson gson = gsonBuilder.setPrettyPrinting().create();
        String result = "";

        // parse each node to string
        for (int i = 0; i < nodeArray.size(); i++) {
            result += gson.toJson(nodeArray.get(i)) + ",,"; // use ,, as delimiter to separate serialized nodes in string
        }
        System.out.println(result);
        return result;
    }
    //传进来的一个String(json),然后分解字符串，再deserializing一个一个node加入列表，最后返回列表
    public List<Node> deserializing(String serializedString){
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new springredis.demo.serializer.LocalDateTimeDeserializer());
        Gson gson = gsonBuilder.setPrettyPrinting().create();

        String[] stringArray = serializedString.split(",,");
        List<Node> nodeArray = Arrays.asList(new Node [stringArray.length]); // Create a node list with # of node size

        // parse each string to node
        for (int i = 0; i < nodeArray.size(); i++) {
            nodeArray.set(i, gson.fromJson(stringArray[i], Node.class));
        }

        return nodeArray;
    }
}


