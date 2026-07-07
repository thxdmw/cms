package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizLink;

import java.util.List;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizLinkService extends IService<BizLink> {

    List<BizLink> selectLinks(BizLink bizLink);

    IPage<BizLink> pageLinks(BizLink bizLink, Integer pageNumber, Integer pageSize);

    int deleteBatch(String[] ids);

}
