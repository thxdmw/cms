package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameSnapshotFile;
import org.apache.ibatis.annotations.Mapper;

/** 快照文件清单数据访问层。 */
@Mapper
public interface GameSnapshotFileMapper extends BaseMapper<GameSnapshotFile> {
}
