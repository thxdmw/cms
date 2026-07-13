package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameLibrary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** GameSave 游戏库数据访问层。 */
@Mapper
public interface GameLibraryMapper extends BaseMapper<GameLibrary> {

    @Select("SELECT * FROM game_library WHERE game_id = #{gameId} AND user_id = #{userId} AND status = 1 LIMIT 1")
    GameLibrary selectActiveOwned(@Param("gameId") String gameId, @Param("userId") String userId);

    /** 查询指定用户仍处于有效状态的游戏，用于快照保留策略。 */
    @Select("SELECT * FROM game_library WHERE game_id = #{gameId} AND user_id = #{userId} AND status = 1 LIMIT 1")
    GameLibrary selectOwnedForRetention(@Param("gameId") String gameId, @Param("userId") String userId);

    @Select("SELECT * FROM game_library WHERE user_id = #{userId} AND LOWER(name) = LOWER(#{name}) AND status = 1 LIMIT 1")
    GameLibrary selectActiveByName(@Param("userId") String userId, @Param("name") String name);

    @Select("SELECT * FROM game_library WHERE status = 1 AND retention_enabled = 1")
    java.util.List<GameLibrary> selectRetentionEnabledGames();

    @Update("UPDATE game_library SET retention_enabled = #{enabled}, retention_count = #{count}, retention_days = #{days} WHERE game_id = #{gameId} AND user_id = #{userId} AND status = 1")
    int updateRetentionPolicy(@Param("gameId") String gameId,
                              @Param("userId") String userId,
                              @Param("enabled") int enabled,
                              @Param("count") Integer count,
                              @Param("days") Integer days);

    @Update("UPDATE game_library SET status = 0 WHERE game_id = #{gameId} AND user_id = #{userId} AND status = 1")
    int markDeleted(@Param("gameId") String gameId, @Param("userId") String userId);
}