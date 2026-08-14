package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameCleanupTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GameCleanupTaskMapper extends BaseMapper<GameCleanupTask> {

    @Select("SELECT * FROM game_cleanup_task "
            + "WHERE status IN ('PENDING','FAILED') "
            + "OR (status = 'RUNNING' AND (lease_until IS NULL OR lease_until < NOW())) "
            + "ORDER BY update_time ASC, id ASC LIMIT #{limit}")
    List<GameCleanupTask> selectRunnable(@Param("limit") int limit);

    @Update("UPDATE game_cleanup_task SET status = 'RUNNING', worker_id = #{workerId}, "
            + "lease_until = DATE_ADD(NOW(), INTERVAL #{leaseSeconds} SECOND), "
            + "last_heartbeat_time = NOW(), last_error = NULL "
            + "WHERE task_id = #{taskId} AND (status IN ('PENDING','FAILED') "
            + "OR (status = 'RUNNING' AND (lease_until IS NULL OR lease_until < NOW())))")
    int claim(@Param("taskId") String taskId,
              @Param("workerId") String workerId,
              @Param("leaseSeconds") int leaseSeconds);

    @Update("UPDATE game_cleanup_task SET lease_until = DATE_ADD(NOW(), INTERVAL #{leaseSeconds} SECOND), "
            + "last_heartbeat_time = NOW() WHERE task_id = #{taskId} AND status = 'RUNNING' "
            + "AND worker_id = #{workerId}")
    int renewLease(@Param("taskId") String taskId,
                   @Param("workerId") String workerId,
                   @Param("leaseSeconds") int leaseSeconds);

    @Update("UPDATE game_cleanup_task SET status = 'PENDING', `cursor` = #{cursor}, last_error = NULL, "
            + "worker_id = NULL, lease_until = NULL WHERE task_id = #{taskId} AND status = 'RUNNING' "
            + "AND worker_id = #{workerId}")
    int advance(@Param("taskId") String taskId,
                @Param("workerId") String workerId,
                @Param("cursor") long cursor);

    @Update("UPDATE game_cleanup_task SET status = 'COMPLETED', last_error = NULL, "
            + "worker_id = NULL, lease_until = NULL WHERE task_id = #{taskId} AND status = 'RUNNING' "
            + "AND worker_id = #{workerId}")
    int complete(@Param("taskId") String taskId, @Param("workerId") String workerId);

    @Update("UPDATE game_cleanup_task SET status = 'FAILED', retry_count = retry_count + 1, "
            + "last_error = LEFT(#{error}, 1000), worker_id = NULL, lease_until = NULL "
            + "WHERE task_id = #{taskId} AND status = 'RUNNING' AND worker_id = #{workerId}")
    int fail(@Param("taskId") String taskId,
             @Param("workerId") String workerId,
             @Param("error") String error);

    @Update("UPDATE game_cleanup_task SET task_id = #{taskId}, status = 'PENDING', `cursor` = 0, "
            + "retry_count = 0, last_error = NULL, worker_id = NULL, lease_until = NULL, "
            + "last_heartbeat_time = NULL WHERE user_id = #{userId} AND game_id = #{gameId}")
    int resetForGame(@Param("taskId") String taskId,
                     @Param("userId") String userId,
                     @Param("gameId") String gameId);
}
