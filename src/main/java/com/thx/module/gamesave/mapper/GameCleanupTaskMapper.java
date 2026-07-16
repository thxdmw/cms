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
            + "WHERE status IN ('PENDING','FAILED') ORDER BY update_time ASC, id ASC LIMIT #{limit}")
    List<GameCleanupTask> selectRunnable(@Param("limit") int limit);

    @Update("UPDATE game_cleanup_task SET status = 'RUNNING', last_error = NULL "
            + "WHERE task_id = #{taskId} AND status IN ('PENDING','FAILED')")
    int claim(@Param("taskId") String taskId);

    @Update("UPDATE game_cleanup_task SET status = 'PENDING', `cursor` = #{cursor}, last_error = NULL "
            + "WHERE task_id = #{taskId} AND status = 'RUNNING'")
    int advance(@Param("taskId") String taskId, @Param("cursor") long cursor);

    @Update("UPDATE game_cleanup_task SET status = 'COMPLETED', last_error = NULL "
            + "WHERE task_id = #{taskId} AND status = 'RUNNING'")
    int complete(@Param("taskId") String taskId);

    @Update("UPDATE game_cleanup_task SET status = 'FAILED', retry_count = retry_count + 1, "
            + "last_error = LEFT(#{error}, 1000) WHERE task_id = #{taskId} AND status = 'RUNNING'")
    int fail(@Param("taskId") String taskId, @Param("error") String error);

    @Update("UPDATE game_cleanup_task SET task_id = #{taskId}, status = 'PENDING', `cursor` = 0, "
            + "retry_count = 0, last_error = NULL WHERE user_id = #{userId} AND game_id = #{gameId}")
    int resetForGame(@Param("taskId") String taskId,
                     @Param("userId") String userId,
                     @Param("gameId") String gameId);
}
