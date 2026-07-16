package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameSnapshotFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 快照文件清单数据访问层。 */
@Mapper
public interface GameSnapshotFileMapper extends BaseMapper<GameSnapshotFile> {

    @Select("SELECT * FROM game_snapshot_file WHERE snapshot_id = #{snapshotId} "
            + "ORDER BY id ASC LIMIT #{limit}")
    List<GameSnapshotFile> selectCleanupBatch(@Param("snapshotId") String snapshotId,
                                              @Param("limit") int limit);
}
