package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.module.admin.entity.BizTags;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizTagsMapper extends BaseMapper<BizTags> {

    List<BizTags> selectTags(@Param("page") IPage<BizTags> page, @Param("bizTags") BizTags bizTags);

}
