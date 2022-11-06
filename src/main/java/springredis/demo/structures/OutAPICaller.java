package springredis.demo.structures;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

import springredis.demo.Service.DAO;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Event;
import springredis.demo.entity.Node;
import springredis.demo.entity.TimeTask;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.TimeDelayRepository;
import springredis.demo.tasks.CMTExecutor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

public class OutAPICaller implements Runnable{

	@Autowired
    private TimeDelayRepository timeDelayRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RestTemplate restTemplate = new RestTemplate();
    
    private boolean isRunning = true;
    private String outQueueKey = "OutQueue";
    public String timeKey = "triggerTime";
    public String idKey = "id";
    final String url = "http://localhost:3000";
    private HashMap<String, String> urlDict = new HashMap<String, String>() {{
        put("APITrigger", "   ");
        put("ActionSend", "   ");
        put("TimeDelay", "    ");
        //put("if/else", " ")
    }};
    

    
    public OutAPICaller(TimeDelayRepository timeDelayRepository, RedisTemplate redisTemplate, NodeRepository nodeRepository) {
        this.timeDelayRepository = timeDelayRepository;
        this.redisTemplate = redisTemplate;
        this.nodeRepository = nodeRepository;
    }

    //Debug??? connection with core module?
    @Override
    public void run() {
    	System.out.println("================================================OUTAPI Running=====================================================");
    	System.out.println(redisTemplate.opsForList().size(outQueueKey));
    	while(isRunning = true) {
    		
	        while (redisTemplate.opsForList().size(outQueueKey)>0){
	            //HashMap outEvent = ((HashMap) redisTemplate.opsForList().rightPop(outQueueKey));
	            //Long id = ((Number)outEvent.get(idKey)).longValue();
	        	Event outEvent = ((Event) redisTemplate.opsForList().rightPop(outQueueKey));
	        	Long id = ((Number)outEvent.getId()).longValue();
	            Optional<TimeTask> timeTaskOp = timeDelayRepository.findById(id);
	            System.out.println("================================================OUTAPI successful 0=====================================================");
	            
	            if (timeTaskOp.isPresent()){
	            	System.out.println("================================================OUTAPI successful 1=====================================================");
	//            	TaskExecutor taskExectuor = new TaskExecutor(timeTaskOp.get().getCoreModuleTask());
	            	CoreModuleTask coreModuleTask = timeTaskOp.get().getCoreModuleTask();
	            	System.out.println(coreModuleTask);
	            	Long audienceMoveResult = restTemplate.postForObject("http://localhost:8080/move_user", coreModuleTask, Long.class);  //calls JiaQi's method
	            	System.out.println("=================================CoreModuleTask ID: " + coreModuleTask.getId());
	            	System.out.println("=================================Node ID: " + timeTaskOp.get().getNodeId());
	            	Optional<Node> optionalNode = nodeRepository.findById(timeTaskOp.get().getNodeId());  //retrieves node from repository
	            	
	            	if(optionalNode.isPresent()) {
	            		System.out.println("================================================OUTAPI successful 2=====================================================");
	            		Node node = optionalNode.get();
	            		node.nextsDeserialize();  //convert the nexts from string to list
	            		System.out.println("Node getNexts Index 0 =======================" + node.getNexts().get(0));
	            		Optional<Node> optionalNextNode = nodeRepository.findById(node.getNexts().get(0));  //find next node by id from repository
	            		System.out.println("Is nextNode present==================================" + optionalNextNode.isPresent());
	            		
	            		if(optionalNextNode.isPresent()) {
	            			Node nextNode = optionalNextNode.get();
	            			CoreModuleTask nextCoreModuleTask = new CoreModuleTask(coreModuleTask);  //create new CoreModuleTask based on current CoreModuleTask
	            			nextCoreModuleTask.setType(nextNode.getType());
	            			nextCoreModuleTask.setName(nextNode.getName());
	            			
	            			//This information will be lost when saved into DB. Does CoreModuleTask need its own attributes for nodeId and audience?
	            			nextCoreModuleTask.setNodeId(nextNode.getId());  //set the node id to next node
	            			nextCoreModuleTask.setActiveAudienceId1(timeTaskOp.get().activeAudienceId1SSerialize());
	            			nextCoreModuleTask.setActiveAudienceId2(timeTaskOp.get().activeAudienceId2SSerialize());
	            			nextCoreModuleTask.setAudienceId1(timeTaskOp.get().audienceId1SSerialize());
	            			nextCoreModuleTask.setAudienceId2(timeTaskOp.get().audienceId2SSerialize());
	            			
	            			//auditing support
	            			nextCoreModuleTask.setCreatedAt(LocalDateTime.now());
	            			nextCoreModuleTask.setCreatedBy("TimeModule");
	            			
//	            			nextCoreModuleTask.getActiveAudienceId1().add(audienceMoveResult);  //set the active audience id to the one returned by JiaQi's method
//	            			nextCoreModuleTask.setSourceNodeId(coreModuleTask.getNodeId()); //set the source node id to that of the current node's id
//	            			nextCoreModuleTask.setTargetNodeId(nextNode.getNexts().get(0));  //set the target node id to that of the next node of nextNode
	            			Long addTaskResult = restTemplate.postForObject("http://localhost:8080/ReturnTask", nextCoreModuleTask, Long.class);  //using JiaQi's method	
	            			System.out.println("================================================OUTAPI successful 3=====================================================");
	            		}
	            	}
	            	
	            	String type = coreModuleTask.getType();
	            	String url = urlDict.get(type);
	            	String result = restTemplate.postForObject(url, timeTaskOp.get(), String.class);
	            }
	
	        }
    	}

    }
    
    
    
}