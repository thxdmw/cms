package com.thx.module.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizCategory;

import java.util.List;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizCategoryService extends IService<BizCategory> {

    List<BizCategory> selectCategories(BizCategory bizCategory);

    int deleteBatch(String[] ids);

    BizCategory selectById(String id);

    List<BizCategory> selectByPid(String pid);

}
