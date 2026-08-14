package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** GameSave 用户账号数据访问层。 */
@Mapper
public interface GameAccountMapper extends BaseMapper<GameAccount> {

    /** 在单条 SQL 中校验剩余配额并预占容量，避免并发上传绕过配额上限。 */
    @Update("UPDATE game_account SET used_bytes = used_bytes + #{bytes} "
            + "WHERE user_id = #{userId} AND status = 1 AND #{bytes} >= 0 "
            + "AND (quota_bytes IS NULL OR (#{bytes} <= quota_bytes AND used_bytes <= quota_bytes - #{bytes}))")
    int reserveQuota(@Param("userId") String userId, @Param("bytes") long bytes);

    /** 仅在已用容量足够时释放，禁止计数减为负数。 */
    @Update("UPDATE game_account SET used_bytes = used_bytes - #{bytes} "
            + "WHERE user_id = #{userId} AND status = 1 AND #{bytes} >= 0 AND used_bytes >= #{bytes}")
    int releaseQuota(@Param("userId") String userId, @Param("bytes") long bytes);}
