package springredis.demo.serializer;

import com.google.gson.Gson;
import springredis.demo.entity.Node;

import java.util.ArrayList;

public class SeDeFunction {
    //传进来的是node list，把一个一个node拿出来序列化，然后加入String，返回String
    public String serializing(ArrayList<Node> node_list){
        Gson gson = new Gson();
        String result="";
        while (!node_list.isEmpty()){
            Node node1=node_list.remove(0);
            result=result+gson.toJson(node1)+" ";
        }
        return result;
    }
    //传进来的一个String(json),然后分解字符串，再deserializing一个一个node加入列表，最后返回列表
    public ArrayList<Node> deserializing(String jsonString){
        Gson gson=new Gson();
        String[] strs=jsonString.split(" ");
        ArrayList<Node> node_list=new ArrayList<>();
        for (int i = 0; i < strs.length; i++) {
            Node node=gson.fromJson(strs[i],Node.class);
            node_list.add(node);
        }
        return node_list;
    }
}