package springredis.demo.serializer;

import com.google.gson.*;
import springredis.demo.entity.Node;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SeDeFunction {
    //传进来的是node list，把一个一个node拿出来序列化，然后加入String，返回String
    GsonBuilder gsonBuilder = new GsonBuilder();

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
class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d::MMM::uuuu HH::mm::ss");
    @Override
    public JsonElement serialize(LocalDateTime localDateTime, Type srcType, JsonSerializationContext context) {
        return new JsonPrimitive(formatter.format(localDateTime));
    }
}

class LocalDateTimeDeserializer implements JsonDeserializer < LocalDateTime > {
    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return LocalDateTime.parse(json.getAsString(),
                DateTimeFormatter.ofPattern("d::MMM::uuuu HH::mm::ss").withLocale(Locale.ENGLISH));
    }
}