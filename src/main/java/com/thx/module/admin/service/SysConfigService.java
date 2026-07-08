package com.thx.module.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.SysConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 系统配置服务，管理键值对形式的站点参数配置（如站点静态化开关等）。
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 查询全部系统配置，转换为 key-value 形式，结果做了缓存（固定 key "config"）。
     *
     * @return key 为配置项 key，value 为配置值
     */
    Map<String, String> selectAll();

    /**
     * 按 key 更新配置值：已存在该 key 则更新，不存在则新增；成功后清空配置缓存。
     *
     * @param key   配置项 key
     * @param value 配置值
     * @return 是否更新/新增成功
     */
    boolean updateByKey(String key, String value);

    /**
     * 批量更新配置：逐个 key-value 调用 {@link #updateByKey(String, String)}。
     * request/response 参数当前未在实现中使用，为预留扩展参数。
     *
     * @param map      待更新的配置键值对
     * @param request  当前请求（预留）
     * @param response 当前响应（预留）
     */
    void updateAll(Map<String, String> map, HttpServletRequest request, HttpServletResponse response);
}
