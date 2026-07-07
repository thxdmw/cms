package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.BizLove;
import org.apache.ibatis.annotations.Param;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizLoveMapper extends BaseMapper<BizLove> {

    BizLove checkLove(@Param("bizId") String bizId, @Param("userIp") String userIp);
}
