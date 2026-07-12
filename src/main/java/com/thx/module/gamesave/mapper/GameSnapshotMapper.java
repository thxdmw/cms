package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 不可变快照数据访问层。 */
@Mapper
public interface GameSnapshotMapper extends BaseMapper<GameSnapshot> {

    /** 仅将仍处于 ACTIVE 状态、且归属匹配的历史快照标记为删除，避免并发重复回收对象引用。 */
    @Update("UPDATE game_snapshot SET status = 'DELETED' "
            + "WHERE snapshot_id = #{snapshotId} AND user_id = #{userId} "
            + "AND game_id = #{gameId} AND status = 'ACTIVE'")
    int markDeleted(@Param("snapshotId") String snapshotId,
                    @Param("userId") String userId,
                    @Param("gameId") String gameId);
    /** 按新到旧读取指定游戏的全部 ACTIVE 快照，供保留策略判断。 */
    @Select("SELECT * FROM game_snapshot "
            + "WHERE user_id = #{userId} AND game_id = #{gameId} AND status = 'ACTIVE' "
            + "ORDER BY create_time DESC, id DESC LIMIT 2000")
    List<GameSnapshot> selectActiveForRetention(@Param("userId") String userId,
                                                @Param("gameId") String gameId);}
