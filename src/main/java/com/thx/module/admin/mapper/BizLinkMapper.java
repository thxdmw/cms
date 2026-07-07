package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.module.admin.entity.BizLink;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizLinkMapper extends BaseMapper<BizLink> {

    List<BizLink> selectLinks(@Param("page") IPage<BizLink> page, @Param("bizLink") BizLink bizLink);

    int deleteBatch(String[] ids);

}
