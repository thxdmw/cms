package com.thx.module.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizTheme;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizThemeService extends IService<BizTheme> {

    int useTheme(String id);

    BizTheme selectCurrent();

    int deleteBatch(String[] ids);

}
