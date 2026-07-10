package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameLibrary;
import org.apache.ibatis.annotations.Mapper;

/** 用户逻辑游戏数据访问层。 */
@Mapper
public interface GameLibraryMapper extends BaseMapper<GameLibrary> {
}
