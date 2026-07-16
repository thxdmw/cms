package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/** GameSave 设备数据访问层。 */
@Mapper
public interface GameDeviceMapper extends BaseMapper<GameDevice> {

    /** 仅撤销归属当前用户且仍启用的目标设备，避免跨用户或重复撤销。 */
    @Update("UPDATE game_device SET status = 0 "
            + "WHERE device_id = #{deviceId} AND user_id = #{userId} AND status = 1")
    int revokeActiveDevice(@Param("deviceId") String deviceId,
                           @Param("userId") String userId);

    @Update("UPDATE game_device SET last_seen_time = #{now} "
            + "WHERE id = #{id} AND status = 1 "
            + "AND (last_seen_time IS NULL OR last_seen_time < #{threshold})")
    int touchLastSeenIfStale(@Param("id") Long id,
                             @Param("now") Date now,
                             @Param("threshold") Date threshold);
}
