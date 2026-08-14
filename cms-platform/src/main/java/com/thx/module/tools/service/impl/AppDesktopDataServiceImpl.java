package com.thx.module.tools.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.module.tools.entity.AppDesktopData;
import com.thx.module.tools.mapper.AppDesktopDataMapper;
import com.thx.module.tools.service.AppDesktopDataService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * {@link AppDesktopDataService} 的默认实现，直接继承 MyBatis-Plus 的 {@link ServiceImpl}，
 * 不需要额外编写任何逻辑即可获得针对 {@link AppDesktopData} 的完整增删改查能力。
 */
@Service
public class AppDesktopDataServiceImpl extends ServiceImpl<AppDesktopDataMapper, AppDesktopData> implements AppDesktopDataService {


}
