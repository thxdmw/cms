package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** GameSave 设备数据访问层。 */
@Mapper
public interface GameDeviceMapper extends BaseMapper<GameDevice> {

    /** 仅撤销归属当前用户且仍启用的目标设备，避免跨用户或重复撤销。 */
    @Update("UPDATE game_device SET status = 0 "
            + "WHERE device_id = #{deviceId} AND user_id = #{userId} AND status = 1")
    int revokeActiveDevice(@Param("deviceId") String deviceId,
                           @Param("userId") String userId);}
