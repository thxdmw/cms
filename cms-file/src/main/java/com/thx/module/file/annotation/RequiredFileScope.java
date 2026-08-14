package com.thx.module.file.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明文件系统接口所需的 Scope。
 * FileAuthInterceptor 只放行标注了该注解且调用方 Scope 匹配的请求；
 * 未标注该注解的 /api/v1/files/** 接口一律拒绝访问（Fail Closed）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredFileScope {

    /** 所需的 Scope 名称，如 UPLOAD/READ/DELETE/LIST/PRESIGN，需与 file_app.scopes 中的取值一致 */
    String value();
}
