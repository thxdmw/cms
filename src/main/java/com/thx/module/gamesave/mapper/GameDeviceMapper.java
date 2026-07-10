package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameDevice;
import org.apache.ibatis.annotations.Mapper;

/** GameSave 设备数据访问层。 */
@Mapper
public interface GameDeviceMapper extends BaseMapper<GameDevice> {
}
