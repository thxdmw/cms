package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizTags;

import java.util.List;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizTagsService extends IService<BizTags> {

    List<BizTags> selectTags(BizTags bizTags);

    IPage<BizTags> pageTags(BizTags bizTags, Integer pageNumber, Integer pageSize);

    int deleteBatch(String[] ids);
}
