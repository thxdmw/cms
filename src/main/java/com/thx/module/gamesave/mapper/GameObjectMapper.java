package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.dto.ObjectDescriptor;
import com.thx.module.gamesave.model.GameObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** GameSave 内容对象数据访问层。 */
@Mapper
public interface GameObjectMapper extends BaseMapper<GameObject> {

    /**
     * 按 (sha256, size) 批量查询当前用户仍处于 ACTIVE 的内容对象，一次查询替代逐个对象的判重/归属校验。
     * 调用方必须保证 descriptors 非空，避免生成空 IN 列表。
     */
    @Select("<script>"
            + "SELECT * FROM game_object WHERE user_id = #{userId} AND status = 'ACTIVE' AND (sha256, size) IN "
            + "<foreach item='item' collection='descriptors' open='(' separator=',' close=')'>"
            + "(#{item.sha256}, #{item.size})"
            + "</foreach>"
            + "</script>")
    List<GameObject> selectActiveByDescriptors(@Param("userId") String userId,
                                               @Param("descriptors") List<ObjectDescriptor> descriptors);

    /** 仅允许给当前用户仍处于 ACTIVE 的对象增加快照引用计数。 */
    @Update("UPDATE game_object SET reference_count = reference_count + 1 "
            + "WHERE object_id = #{objectId} AND user_id = #{userId} AND status = 'ACTIVE'")
    int incrementReference(@Param("objectId") String objectId, @Param("userId") String userId);

    /** 只允许释放仍有快照引用的 ACTIVE 对象，避免引用计数变成负数。 */
    @Update("UPDATE game_object SET reference_count = reference_count - 1 "
            + "WHERE object_id = #{objectId} AND user_id = #{userId} "
            + "AND status = 'ACTIVE' AND reference_count > 0")
    int decrementReference(@Param("objectId") String objectId, @Param("userId") String userId);

    /** 零引用对象只进入待清理状态，底层文件删除由短事务之外的后台任务完成。 */
    @Update("UPDATE game_object SET status = 'DELETING' "
            + "WHERE object_id = #{objectId} AND user_id = #{userId} "
            + "AND status = 'ACTIVE' AND reference_count = 0")
    int markDeletingIfUnreferenced(@Param("objectId") String objectId,
                                   @Param("userId") String userId);

    @Select("SELECT * FROM game_object WHERE user_id = #{userId} AND sha256 = #{sha256} AND size = #{size} LIMIT 1")
    GameObject selectAnyByDescriptor(@Param("userId") String userId,
                                     @Param("sha256") String sha256,
                                     @Param("size") long size);

    @Update("UPDATE game_object SET file_id = #{newFileId}, reference_count = 0, status = 'ACTIVE' "
            + "WHERE id = #{id} AND user_id = #{userId} AND status = 'DELETED'")
    int reactivateDeleted(@Param("id") Long id,
                          @Param("userId") String userId,
                          @Param("newFileId") String newFileId);

    @Select("SELECT * FROM game_object WHERE status = 'DELETING' ORDER BY update_time ASC, id ASC LIMIT #{limit}")
    List<GameObject> selectDeletingBatch(@Param("limit") int limit);

    @Select("SELECT * FROM game_object WHERE status = 'ACTIVE' AND reference_count = 0 "
            + "AND create_time < #{threshold} ORDER BY create_time ASC, id ASC LIMIT #{limit}")
    List<GameObject> selectOrphanCandidates(@Param("threshold") java.util.Date threshold,
                                            @Param("limit") int limit);

    @Update("UPDATE game_object SET status = 'DELETED' "
            + "WHERE object_id = #{objectId} AND user_id = #{userId} "
            + "AND status = 'DELETING' AND reference_count = 0")
    int markDeletedFromDeleting(@Param("objectId") String objectId,
                                @Param("userId") String userId);
}
