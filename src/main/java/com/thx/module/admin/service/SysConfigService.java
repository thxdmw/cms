package com.thx.module.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.SysConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface SysConfigService extends IService<SysConfig> {

    Map<String, String> selectAll();

    boolean updateByKey(String key, String value);

    void updateAll(Map<String, String> map, HttpServletRequest request, HttpServletResponse response);
}
