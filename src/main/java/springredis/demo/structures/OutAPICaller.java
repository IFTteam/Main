package springredis.demo.structures;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

import springredis.demo.DemoApplication;
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
import springredis.demo.repository.activeRepository.ActiveNodeRepository;
import springredis.demo.tasks.CMTExecutor;

import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class OutAPICaller implements Runnable{

	@Autowired
    private TimeDelayRepository timeDelayRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RestTemplate restTemplate = new RestTemplate();
	@Autowired
	CMTExecutor cmtExecutor;

    private boolean isRunning = true;
    private String outQueueKey = "OutQueue";
    public String timeKey = "triggerTime";
    public String idKey = "id";
    final String url = "http://localhost:3000";
    private HashMap<String, String> urlDict = new HashMap<String, String>() {{
		put("Time Delay", "http://localhost:8080/Time_Delay");
		put("API Trigger", "http://localhost:8080/API_trigger");
		put("Time Trigger", "http://localhost:8080/add");
		put("Send Email", "http://localhost:8080/actionSend/createCMTTransmission");
		put("If/Else", "http://localhost:8080/IfElse");
		put("Add Tag", "http://localhost:8080/AddTag");
        //put("APITrigger", "   ");
        //put("ActionSend", "   ");
        //put("TimeDelay", "http://localhost:8080/add");
        //put("if/else", " ")
    }};



	public OutAPICaller(TimeDelayRepository timeDelayRepository, RedisTemplate redisTemplate, NodeRepository nodeRepository, ActiveNodeRepository activeNodeRepository) {
		this.timeDelayRepository = timeDelayRepository;
		this.redisTemplate = redisTemplate;
		this.nodeRepository = nodeRepository;
		cmtExecutor = new CMTExecutor(nodeRepository, restTemplate, activeNodeRepository);
	}

    @SneakyThrows
	@Override
    public void run() {
    	System.out.println("================================================OutAPI Starts=====================================================");
    	System.out.println(redisTemplate.opsForList().size(outQueueKey));
    	while(isRunning = true) {
			try {
				while (redisTemplate.opsForList().size(outQueueKey) > 0){
					//HashMap outEvent = ((HashMap) redisTemplate.opsForList().rightPop(outQueueKey));
					//Long id = ((Number)outEvent.get(idKey)).longValue();
					Event outEvent = ((Event) redisTemplate.opsForList().rightPop(outQueueKey));
					Long id = ((Number)outEvent.getId()).longValue();
					System.out.println("the id is: " + id);
					Optional<TimeTask> timeTaskOp = timeDelayRepository.findById(id);
					if (timeTaskOp.isEmpty()) {
						throw new DataBaseObjectNotFoundException("No Time Task Exist");
					}
					TimeTask timetask = timeTaskOp.get();
					timetask.audience_serialize();
					System.out.println("================================================Time Task retrieved=====================================================");
//            	TaskExecutor taskExectuor = new TaskExecutor(timeTaskOp.get().getCoreModuleTask());
					CoreModuleTask coreModuleTask = timetask.getCoreModuleTask();

					coreModuleTask.setMakenext(timetask.getMakenext());

					System.out.println(coreModuleTask.getAudienceId1());
					System.out.println("cur JI id is:" + timetask.getJourneyId());
					coreModuleTask = restTemplate.postForObject("http://localhost:8080/move_user", coreModuleTask, CoreModuleTask.class);  //calls JiaQi's method
					System.out.println("=================================CoreModuleTask ID: " + coreModuleTask.getId());
					System.out.println("=================================Node: " + timetask);
					Optional<Node> optionalNode = nodeRepository.findById(timetask.getNodeId());  //retrieves node from repository
					if (optionalNode.isEmpty()) {
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
					if (optionalNextNode.isEmpty()) {
						throw new DataBaseObjectNotFoundException("The Next node does not exist");
					}

					Node nextNode = initializeNodeFromDB(optionalNextNode);

					CoreModuleTask nextCoreModuleTask = new CoreModuleTask(coreModuleTask);  //create new CoreModuleTask based on current CoreModuleTask
					System.out.println("cur CM id is:" + coreModuleTask);

					nextCoreModuleTask.setType(nextNode.getType());
					nextCoreModuleTask.setName(nextNode.getName());
					//This information will be lost when saved into DB. Does CoreModuleTask need its own attributes for nodeId and audience?
					System.out.println("next node id is:" + nextNode.getId());
					nextCoreModuleTask.setJourneyId(timetask.getJourneyId());
					nextCoreModuleTask.setNodeId(nextNode.getId());  //set the node id to next node
//            	nextCoreModuleTask.setActiveAudienceId1(timeTaskOp.get().activeAudienceId1SSerialize());
//            	nextCoreModuleTask.setActiveAudienceId2(timeTaskOp.get().activeAudienceId2SSerialize());
					System.out.println("TTAA1 info" + timetask.getActiveAudienceId1());
					System.out.println("TTAA2 info" + timetask.getActiveAudienceId2());
					System.out.println("TTA1 info" + timetask.getAudienceId1());
					System.out.println("TTA2 info" + timetask.getAudienceId2());
					nextCoreModuleTask.setActiveAudienceId1(timetask.activeAudienceId1SSerialize());
					nextCoreModuleTask.setActiveAudienceId2(timetask.audienceId2SSerialize());
					nextCoreModuleTask.setAudienceId1(timetask.audienceId1SSerialize());
					nextCoreModuleTask.setAudienceId2(timetask.audienceId2SSerialize());
					System.out.println("NAA1 info" + nextCoreModuleTask.getActiveAudienceId1());
					System.out.println("NAA2 info" + nextCoreModuleTask.getActiveAudienceId2());
					System.out.println("NA1 info" + nextCoreModuleTask.getAudienceId1());
					System.out.println("NA2 info" + nextCoreModuleTask.getAudienceId2());
					//auditing support
					nextCoreModuleTask.setCreatedAt(LocalDateTime.now());
					nextCoreModuleTask.setCreatedBy("TimeModule");

//            	nextCoreModuleTask.getActiveAudienceId1().add(audienceMoveResult);  //set the active audience id to the one returned by JiaQi's method
//          	nextCoreModuleTask.setSourceNodeId(coreModuleTask.getNodeId()); //set the source node id to that of the current node's id
//            	nextCoreModuleTask.setTargetNodeId(nextNode.getNexts().get(0));  //set the target node id to that of the next node of nextNode
					//Long addTaskResult = restTemplate.postForObject("http://localhost:8080/ReturnTask", nextCoreModuleTask, Long.class);  //using JiaQi's method
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
					ArrayList array = new ArrayList<Long>(1501);
					//coreModuleTask.setAudienceId1(array);
//	            String result = restTemplate.postForObject(url, coreModuleTask, String.class);
					cmtExecutor.execute(coreModuleTask);
				}
			} catch (Exception e) {
				Logger logger =LoggerFactory.getLogger(OutAPICaller.class);
				logger.error("error log:"+e);
				run();
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