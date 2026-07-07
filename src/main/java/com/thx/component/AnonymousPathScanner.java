package com.thx.component;

import com.thx.common.annotation.AnonymousAccess;
import com.thx.module.admin.pojo.RequestPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class AnonymousPathScanner implements ApplicationListener<ContextRefreshedEvent> {

    private final Set<RequestPath> anonymousPaths = new HashSet<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        RequestMappingHandlerMapping mapping = event.getApplicationContext().getBean(RequestMappingHandlerMapping.class);

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mapping.getHandlerMethods().entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            boolean hasAnonymous = handlerMethod.hasMethodAnnotation(AnonymousAccess.class) ||
                                    handlerMethod.getBeanType().isAnnotationPresent(AnonymousAccess.class);
            if (!hasAnonymous) continue;

            RequestMappingInfo info = entry.getKey();
            Set<String> patterns = info.getPatternValues();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();

            for (String pattern : patterns) {
                if (methods.isEmpty()) {
                    // 如果没声明 method，表示支持所有请求方法
                    for (RequestMethod method : RequestMethod.values()) {
                        anonymousPaths.add(new RequestPath(method.name(), pattern));
                    }
                } else {
                    for (RequestMethod method : methods) {
                        anonymousPaths.add(new RequestPath(method.name(), pattern));
                    }
                }
            }
        }
        log.info("已扫描到 {} 个匿名访问路径", anonymousPaths.size());
    }

    public boolean isAnonymous(String method, String uri) {
        AntPathMatcher matcher = new AntPathMatcher();
        return anonymousPaths.stream().anyMatch(p ->
                p.getMethod().equalsIgnoreCase(method) && matcher.match(p.getPattern(), uri));
    }
}
