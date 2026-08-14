package com.thx.module.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizTheme;

/**
 * 前台主题（皮肤）服务。同一时间约定只有一个主题处于启用状态。
 */
public interface BizThemeService extends IService<BizTheme> {

    /**
     * 切换当前启用主题：先将所有主题置为未启用，再将指定 id 的主题置为启用；成功后清空主题缓存。
     *
     * @param id 待启用的主题 id
     * @return 影响行数
     */
    int useTheme(String id);

    /**
     * 查询当前启用中的主题，结果做了缓存（固定 key "current"）。
     *
     * @return 当前启用的主题；不存在则为 null
     */
    BizTheme selectCurrent();

    /**
     * 批量删除主题，成功后清空主题缓存。
     *
     * @param ids 主题 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);

}
