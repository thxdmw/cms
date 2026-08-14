package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameSnapshotRoot;
import org.apache.ibatis.annotations.Mapper;

/** 快照存档根目录元数据访问层。 */
@Mapper
public interface GameSnapshotRootMapper extends BaseMapper<GameSnapshotRoot> {
}
