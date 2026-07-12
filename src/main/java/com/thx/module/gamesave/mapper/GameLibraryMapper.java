package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameLibrary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 用户逻辑游戏数据访问层。 */
@Mapper
public interface GameLibraryMapper extends BaseMapper<GameLibrary> {

    /** 查询当前用户拥有的启用游戏及其保留策略。 */
    @Select("SELECT * FROM game_library "
            + "WHERE game_id = #{gameId} AND user_id = #{userId} AND status = 1 LIMIT 1")
    GameLibrary selectOwnedForRetention(@Param("gameId") String gameId,
                                        @Param("userId") String userId);

    /** 查询已启用自动保留清理的游戏，供后台任务分批处理。 */
    @Select("SELECT * FROM game_library WHERE status = 1 AND retention_enabled = 1")
    List<GameLibrary> selectRetentionEnabledGames();

    /** 原子更新当前用户指定游戏的保留策略。 */
    @Update("UPDATE game_library SET retention_enabled = #{enabled}, "
            + "retention_count = #{retentionCount}, retention_days = #{retentionDays} "
            + "WHERE game_id = #{gameId} AND user_id = #{userId} AND status = 1")
    int updateRetentionPolicy(@Param("gameId") String gameId,
                              @Param("userId") String userId,
                              @Param("enabled") int enabled,
                              @Param("retentionCount") int retentionCount,
                              @Param("retentionDays") int retentionDays);}
