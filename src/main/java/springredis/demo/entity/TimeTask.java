package springredis.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.sql.In;
import springredis.demo.entity.base.BaseTaskEntity;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;



@Entity
@Data
@NoArgsConstructor
@Table(name="time_task")
public class TimeTask extends BaseTaskEntity {
    @Id
    @GeneratedValue
    @Column(name="id")
    private Long id;
    
    //Taken from BaseTaskEntity
    private Long nodeId;
    private String activeAudienceId1S;
    private String activeAudienceId2S;
    private String audienceId1S;
    private String audienceId2S;

    private Integer repeatTimes;
    private String repeatInterval;

    private Long triggerTime;

    private int callapi = 0;

    private Long journeyId;

    private Long userId;
    private int makenext = 0;

    private Integer taskStatus;
    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "core_module_task", referencedColumnName = "id")
    private CoreModuleTask coreModuleTask;


//	public CoreModuleTask getCoreModuleTask;
 
    public TimeTask(BaseTaskEntity baseTaskEntity){
        super(baseTaskEntity);
    }
    
    public CoreModuleTask getCoreModuleTask() {
    	return coreModuleTask;
    }

    public Long getTriggerTime() {
    	return this.triggerTime;
    }
    
    public Long getId() {
    	return this.id;
    }
    
    public void setTaskStatus(int taskStatus) {
    	this.taskStatus = taskStatus;
    }
    
    //activeAudienceId1. Make Sure that sNexts is not empty when call this. Converts string to list
  public List<Long> activeAudienceId1SSerialize(){
      List<Long> activeAudienceId1List = new ArrayList<>();
      String[] s = activeAudienceId1S.split(" ");
      for (String value : s) {
          if(!value.isEmpty()){
              activeAudienceId1List.add(Long.parseLong(value));
          }
      }
      return activeAudienceId1List;
  }
  //activeAudienceId1. Converts list to string
  public void activeAudienceId1SDeserialize(List<Long> activeAudienceId1List){
      StringBuffer buffer = new StringBuffer();
      for(Long num:activeAudienceId1List){
          buffer.append(num.toString() + " ");
      }
      activeAudienceId1S = buffer.toString();
  }
  
  //activeAudienceId2. Make Sure that sNexts is not empty when call this. Converts string to list
  public List<Long> activeAudienceId2SSerialize(){
      List<Long> activeAudienceId2List = new ArrayList<>();
      String[] s = activeAudienceId2S.split(" ");
      for (String value : s) {
          if(!value.isEmpty()){
              activeAudienceId2List.add(Long.parseLong(value));
          }
      }
      return activeAudienceId2List;
  }
  //activeAudienceId2. Converts list to string
  public void activeAudienceId2SDeserialize(List<Long> activeAudienceId2List){
      StringBuffer buffer = new StringBuffer();
      for(Long num:activeAudienceId2List){
          buffer.append(num.toString() + " ");
      }
      activeAudienceId2S = buffer.toString();
  }
  
  //audienceId1. Make Sure that sNexts is not empty when call this. Converts string to list
  public List<Long> audienceId1SSerialize(){
      List<Long> audienceId1List = new ArrayList<>();
      String[] s = audienceId1S.split(" ");
      for (String value : s) {
          if(!value.isEmpty()){
              audienceId1List.add(Long.parseLong(value));
          }
      }
      return audienceId1List;
  }
  //audienceId1. Converts list to string
  public void audienceId1SDeserialize(List<Long> audienceId1List){
      StringBuffer buffer = new StringBuffer();
      for(Long num:audienceId1List){
          buffer.append(num.toString() + " ");
      }
      audienceId1S = buffer.toString();
  }
  
  //audienceId2. Make Sure that sNexts is not empty when call this. Converts string to list
  public List<Long> audienceId2SSerialize(){
      List<Long> audienceId2List = new ArrayList<>();
      String[] s = audienceId2S.split(" ");
      for (String value : s) {
          if(!value.isEmpty()){
              audienceId2List.add(Long.parseLong(value));
          }
      }
      return audienceId2List;
  }
  //audienceId2. Converts list to string
  public void audienceId2SDeserialize(List<Long> audienceId2List){
      StringBuffer buffer = new StringBuffer();
      for(Long num:audienceId2List){
          buffer.append(num.toString() + " ");
      }
      audienceId2S = buffer.toString();
  }

  public void audience_serialize(){
        this.setAudienceId1(this.audienceId1SSerialize());
        this.setAudienceId2(this.audienceId2SSerialize());
        this.setActiveAudienceId1(this.activeAudienceId1SSerialize());
        this.setActiveAudienceId2(this.activeAudienceId2SSerialize());
  }

    //status of the task
    //0-in sql db, 1-in heap, 2-task complete, -1-task cancelled
}