package com.thx.module.gamesave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.gamesave.model.GameAccount;
import org.apache.ibatis.annotations.Mapper;

/** GameSave 用户账号数据访问层。 */
@Mapper
public interface GameAccountMapper extends BaseMapper<GameAccount> {
}
