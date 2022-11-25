package springredis.demo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

//public class Event implements Serializable{
//This is the data format in the heap
@Embeddable
@Data
@AllArgsConstructor
@Builder
public class Event implements Serializable {
    private Date triggerTime;
    private Long id;

    public Event() {
    	
    }

    public Event(Date triggerTime){
        this.triggerTime = triggerTime;
    }

    public Event(Date triggerTime, Long id) {
        this.triggerTime = triggerTime;
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getTriggerTime() {

        return triggerTime;
    }


    public void setTriggerTime(Date triggerTime) {
        this.triggerTime = triggerTime;
    }

    @JsonProperty("msys")
    private Msys msys;



//    @Override
//    public int compareTo(Event o) {
//        return triggerTime-o.getTriggerTime();
//    }
}