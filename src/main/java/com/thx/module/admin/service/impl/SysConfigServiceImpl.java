package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.common.util.CoreConst;
import com.thx.module.admin.mapper.SysConfigMapper;
import com.thx.module.admin.entity.SysConfig;
import com.thx.module.admin.service.SysConfigService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thx.common.util.CoreConst.SITE_STATIC_KEY;

/**
 * {@link SysConfigService} 实现：站点配置的键值对读写，selectAll() 结果做了缓存
 * （站点设置页多个前台页面都要读，变化不频繁）。
 */
@Service
@AllArgsConstructor
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    /**
     * 启动时把"是否开启静态化"这个配置项同步进 {@link CoreConst#SITE_STATIC} 这个内存标记，
     * 避免请求处理链路上每次都要查一次配置表判断是否走静态化逻辑
     */
    @PostConstruct
    public void init() {
        CoreConst.SITE_STATIC.set("on".equalsIgnoreCase(selectAll().getOrDefault(SITE_STATIC_KEY, "false")));
    }

    @Override
    @Cacheable(value = "site", key = "'config'")
    public Map<String, String> selectAll() {
        List<SysConfig> sysConfigs = sysConfigMapper.selectList(Wrappers.emptyWrapper());
        Map<String, String> map = new HashMap<>(sysConfigs.size());
        for (SysConfig config : sysConfigs) {
            map.put(config.getSysKey(), config.getSysValue());
        }
        return map;
    }

    @Override
    @CacheEvict(value = "site", key = "'config'", allEntries = true)
    public boolean updateByKey(String key, String value) {
        if (getOne(Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getSysKey, key)) != null) {
            return update(Wrappers.<SysConfig>lambdaUpdate().eq(SysConfig::getSysKey, key).set(SysConfig::getSysValue, value));
        } else {
            return save(new SysConfig().setSysKey(key).setSysValue(value));
        }
    }

    @Override
    @CacheEvict(value = "site", key = "'config'", allEntries = true)
    public void updateAll(Map<String, String> map, HttpServletRequest request, HttpServletResponse response) {
        map.forEach(this::updateByKey);
    }
}
