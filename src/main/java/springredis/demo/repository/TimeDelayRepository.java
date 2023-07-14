package springredis.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import springredis.demo.entity.TimeTask;

import java.util.List;

@Repository
public interface TimeDelayRepository extends JpaRepository<TimeTask, Long> {
    @Query(value = "select * from time_task t where t.trigger_time < :trigger_time and t.task_status = 0",nativeQuery = true)
    List<TimeTask> findTasksBeforeTime(@Param("trigger_time") Long triggerTime);

}