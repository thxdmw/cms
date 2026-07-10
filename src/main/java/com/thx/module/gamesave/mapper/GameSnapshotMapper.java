package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameSnapshot;
import org.apache.ibatis.annotations.Mapper;

/** 不可变快照数据访问层。 */
@Mapper
public interface GameSnapshotMapper extends BaseMapper<GameSnapshot> {
}
