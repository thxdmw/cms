package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameSyncHead;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 同步 HEAD 数据访问层，负责初始化和数据库级 CAS 推进。 */
@Mapper
public interface GameSyncHeadMapper extends BaseMapper<GameSyncHead> {

    /** 并发安全地保证 HEAD 行存在；已存在时不修改任何字段。 */
    @Insert("INSERT IGNORE INTO game_sync_head(user_id, game_id, head_snapshot_id, version) "
            + "VALUES(#{userId}, #{gameId}, NULL, 0)")
    int ensureHead(@Param("userId") String userId, @Param("gameId") String gameId);

    /**
     * 仅当数据库当前 HEAD 与客户端 expectedHead 一致时推进。
     * 返回 0 表示期间已有其他设备推进 HEAD，调用方必须按同步冲突处理。
     */
    @Update("<script>"
            + "UPDATE game_sync_head SET head_snapshot_id = #{newHead}, version = version + 1 "
            + "WHERE user_id = #{userId} AND game_id = #{gameId} AND "
            + "<choose>"
            + "<when test='expectedHead == null'>head_snapshot_id IS NULL</when>"
            + "<otherwise>head_snapshot_id = #{expectedHead}</otherwise>"
            + "</choose>"
            + "</script>")
    int advanceHeadCas(@Param("userId") String userId,
                       @Param("gameId") String gameId,
                       @Param("expectedHead") String expectedHead,
                       @Param("newHead") String newHead);

    /** 读取当前 HEAD，保留任务必须无条件保护该快照。 */
    @Select("SELECT head_snapshot_id FROM game_sync_head "
            + "WHERE user_id = #{userId} AND game_id = #{gameId} LIMIT 1")
    String selectHeadSnapshotId(@Param("userId") String userId,
                                @Param("gameId") String gameId);}
