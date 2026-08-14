package com.thx.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring 事务、MyBatis-Plus 分页插件、Mapper 包扫描配置。
 * 各业务模块（admin/tools/file/payment/gamesave）各自维护自己的 mapper 包，统一在这里配置扫描路径。
 */
@EnableTransactionManagement
@Configuration
@MapperScan(basePackages = {"com.thx.module.admin.mapper", "com.thx.module.tools.mapper", "com.thx.module.file.mapper",
        "com.thx.module.payment.repository.mapper", "com.thx.module.gamesave.mapper"})
public class MybatisPlusConfig {

    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor paginationInterceptor() {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        paginationInnerInterceptor.setMaxLimit(-1L);
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(paginationInnerInterceptor);
        return mybatisPlusInterceptor;
    }
}
