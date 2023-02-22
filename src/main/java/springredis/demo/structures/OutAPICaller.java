package springredis.demo.structures;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

import springredis.demo.Service.DAO;
import springredis.demo.entity.CoreModuleTask;
import springredis.demo.entity.Event;
import springredis.demo.entity.Node;
import springredis.demo.entity.TimeTask;
import springredis.demo.entity.base.BaseTaskEntity;
import springredis.demo.error.DataBaseObjectNotFoundException;
import springredis.demo.error.TimeTaskNotExistException;
import springredis.demo.repository.NodeRepository;
import springredis.demo.repository.TimeDelayRepository;
import springredis.demo.tasks.CMTExecutor;

import java.time.LocalDateTime;
import java.util.*;

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
		put("Time Delay", "http://localhost:8080/TimeDelay");
		put("API Trigger", "http://localhost:8080/API_trigger");
		put("Time Trigger", "http://localhost:8080/add");
		put("Send Email", "http://localhost:8080/actionSend/createCMTTransmission");
		put("If/else", "http://localhost:8080/If_Else");
		put("tag", "http://localhost:8080/Tag");
        //put("APITrigger", "   ");
        //put("ActionSend", "   ");
        //put("TimeDelay", "http://localhost:8080/add");
        //put("if/else", " ")
    }};



    public OutAPICaller(TimeDelayRepository timeDelayRepository, RedisTemplate redisTemplate, NodeRepository nodeRepository) {
        this.timeDelayRepository = timeDelayRepository;
        this.redisTemplate = redisTemplate;
        this.nodeRepository = nodeRepository;
    }

    @SneakyThrows
	@Override
    public void run() {
    	System.out.println("================================================OutAPI Starts=====================================================");
    	System.out.println(redisTemplate.opsForList().size(outQueueKey));
    	while(isRunning = true) {

	        while (redisTemplate.opsForList().size(outQueueKey) > 0){
	            //HashMap outEvent = ((HashMap) redisTemplate.opsForList().rightPop(outQueueKey));
	            //Long id = ((Number)outEvent.get(idKey)).longValue();
	        	Event outEvent = ((Event) redisTemplate.opsForList().rightPop(outQueueKey));
	        	Long id = ((Number)outEvent.getId()).longValue();
				System.out.println("the id is: " + id);
	            Optional<TimeTask> timeTaskOp = timeDelayRepository.findById(id);
	            if (!timeTaskOp.isPresent()) {
	            	throw new DataBaseObjectNotFoundException("No Time Task Exist");
				}
				TimeTask timetask = timeTaskOp.get();
            	System.out.println("================================================Time Task retrieved=====================================================");
//            	TaskExecutor taskExectuor = new TaskExecutor(timeTaskOp.get().getCoreModuleTask());
            	CoreModuleTask coreModuleTask = timetask.getCoreModuleTask();
            	System.out.println(coreModuleTask);
            	Long audienceMoveResult = restTemplate.postForObject("http://localhost:8080/move_user", coreModuleTask, Long.class);  //calls JiaQi's method
            	System.out.println("=================================CoreModuleTask ID: " + coreModuleTask.getId());
				System.out.println("=================================Node: " + timetask);
            	Optional<Node> optionalNode = nodeRepository.findById(timetask.getNodeId());  //retrieves node from repository
				if (!optionalNode.isPresent()) {
					throw new DataBaseObjectNotFoundException("The corresponding Time Trigger node does not exist");
				}
				Node node = initializeNodeFromDB(optionalNode);

           		System.out.println("================================================Time Trigger node retrieved=====================================================");

           		//node.nextsDeserialize();
           		//node.setLasts(new ArrayList<>());
				//for now, assume we only have one branch in the journey, so we only take nexts[0]
           		System.out.println("Node getNexts Index 0 =======================" + node.getNexts().get(0));
           		Long next_node_id = node.getNexts().get(0);
           		Optional<Node> optionalNextNode = nodeRepository.findById(next_node_id);  //find next node by id from repository
				if (!optionalNextNode.isPresent()) {
					throw new DataBaseObjectNotFoundException("The Next node does not exist");
				}

           		Node nextNode = initializeNodeFromDB(optionalNextNode);

           		CoreModuleTask nextCoreModuleTask = new CoreModuleTask(coreModuleTask);  //create new CoreModuleTask based on current CoreModuleTask
           		nextCoreModuleTask.setType(nextNode.getType());
           		nextCoreModuleTask.setName(nextNode.getName());
            	//This information will be lost when saved into DB. Does CoreModuleTask need its own attributes for nodeId and audience?
            	nextCoreModuleTask.setNodeId(nextNode.getId());  //set the node id to next node
            	nextCoreModuleTask.setActiveAudienceId1(timeTaskOp.get().activeAudienceId1SSerialize());
            	nextCoreModuleTask.setActiveAudienceId2(timeTaskOp.get().activeAudienceId2SSerialize());
            	nextCoreModuleTask.setAudienceId1(timeTaskOp.get().audienceId1SSerialize());
            	nextCoreModuleTask.setAudienceId2(timeTaskOp.get().audienceId2SSerialize());
				System.out.println("audience info" + timeTaskOp.get().activeAudienceId1SSerialize());
				System.out.println("audience info" + timeTaskOp.get().activeAudienceId2SSerialize());
				System.out.println("audience info" + timeTaskOp.get().audienceId1SSerialize());
				System.out.println("audience info" + timeTaskOp.get().audienceId2SSerialize());
				System.out.println("audience info" + nextCoreModuleTask.getActiveAudienceId1());
				System.out.println("audience info" + nextCoreModuleTask.getActiveAudienceId2());
				System.out.println("audience info" + nextCoreModuleTask.getAudienceId1());
				System.out.println("audience info" + nextCoreModuleTask.getAudienceId2());
            	//auditing support
            	nextCoreModuleTask.setCreatedAt(LocalDateTime.now());
            	nextCoreModuleTask.setCreatedBy("TimeModule");

//            	nextCoreModuleTask.getActiveAudienceId1().add(audienceMoveResult);  //set the active audience id to the one returned by JiaQi's method
//          	nextCoreModuleTask.setSourceNodeId(coreModuleTask.getNodeId()); //set the source node id to that of the current node's id
//            	nextCoreModuleTask.setTargetNodeId(nextNode.getNexts().get(0));  //set the target node id to that of the next node of nextNode
            	Long addTaskResult = restTemplate.postForObject("http://localhost:8080/ReturnTask", nextCoreModuleTask, Long.class);  //using JiaQi's method
            	System.out.println("================================================OUTAPI successful 3=====================================================");
				/*String type = nextCoreModuleTask.getName();
				System.out.println("In outAPI the CM is:" + nextCoreModuleTask);
				System.out.println("In outAPI the type is:" + type);
				String url = urlDict.get(type);
				String result = restTemplate.postForObject(url, nextCoreModuleTask, String.class);
				 */
				coreModuleTask = nextCoreModuleTask;

	            String type = coreModuleTask.getType();
				type = coreModuleTask.getName();
				System.out.println("In outAPI the CM is:" + coreModuleTask);
				System.out.println("In outAPI the type is:" + type);
	            String url = urlDict.get(type);
				System.out.println("the usl is" + url);
				System.out.println("audience info" + coreModuleTask.getActiveAudienceId1());
				System.out.println("audience info" + coreModuleTask.getActiveAudienceId2());
				System.out.println("audience info" + coreModuleTask.getAudienceId1());
				System.out.println("audience info" + coreModuleTask.getAudienceId2());
				ArrayList array = new ArrayList<Long>(1501);
				//coreModuleTask.setAudienceId1(array);
	            String result = restTemplate.postForObject(url, coreModuleTask, String.class);

	        }
    	}

    }

    public Node initializeNodeFromDB(Optional<Node> NodeOp){
    	Node node = NodeOp.get();
		node.nextsDeserialize();
		node.setLasts(new ArrayList<>());
		return node;
	}
    
    
    
}