package com.thx.common.shiro.filter;

import com.thx.common.holder.SpringContextHolder;
import com.thx.infra.AnonymousPathScanner;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.filter.authc.UserFilter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class AnnoOrLoginFilter extends AccessControlFilter {

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        //获取匿名访问路径
        AnonymousPathScanner anonymousPathScanner = SpringContextHolder.getBean(AnonymousPathScanner.class);
        if (anonymousPathScanner.isAnonymous(method, uri)) {
            return true;
        }
        //UserFilter.class 默认的逻辑
        if (isLoginRequest(request, response)) {
            return true;
        } else {
            Subject subject = getSubject(request, response);
            // If principal is not null, then the user is known and should be allowed access.
            return subject.getPrincipal() != null;
        }
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException {
        saveRequestAndRedirectToLogin(request, response);
        return false;
    }


}
