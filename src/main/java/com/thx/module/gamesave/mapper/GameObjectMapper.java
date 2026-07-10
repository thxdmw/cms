package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameObject;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GameObjectMapper extends BaseMapper<GameObject> {
}
