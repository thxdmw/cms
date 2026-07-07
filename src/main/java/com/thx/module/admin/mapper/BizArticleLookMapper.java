package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.BizArticleLook;
import com.thx.module.admin.vo.CountVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizArticleLookMapper extends BaseMapper<BizArticleLook> {

    int checkArticleLook(@Param("articleId") String articleId, @Param("userIp") String userIp, @Param("lookTime") Date lookTime);

    List<CountVo> lookCountByDay(int day);

    List<CountVo> userCountByDay(int day);
}
