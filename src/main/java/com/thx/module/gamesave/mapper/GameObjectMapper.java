package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** GameSave 内容对象数据访问层。 */
@Mapper
public interface GameObjectMapper extends BaseMapper<GameObject> {

    /** 仅允许给当前用户仍处于 ACTIVE 的对象增加快照引用计数。 */
    @Update("UPDATE game_object SET reference_count = reference_count + 1 "
            + "WHERE object_id = #{objectId} AND user_id = #{userId} AND status = 'ACTIVE'")
    int incrementReference(@Param("objectId") String objectId, @Param("userId") String userId);
}
