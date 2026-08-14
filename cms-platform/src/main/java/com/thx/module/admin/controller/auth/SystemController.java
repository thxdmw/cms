package com.thx.module.admin.controller.auth;

import com.thx.common.util.CoreConst;
import com.thx.common.util.ResultUtil;
import com.thx.module.admin.entity.BizCategory;
import com.thx.module.admin.entity.Permission;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.service.BizCategoryService;
import com.thx.module.admin.service.PermissionService;
import com.thx.module.admin.service.SysConfigService;
import com.thx.module.admin.service.UserService;
import com.thx.module.admin.vo.CurrentUserVo;
import com.thx.common.vo.ResponseVo;
import com.pig4cloud.captcha.utils.CaptchaJakartaUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.stp.StpUtil;
import com.thx.common.security.LoginAuthenticator;
import com.thx.common.security.UserContext;
import com.thx.common.util.IpUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台登录鉴权相关接口：登录页展示与提交、登出、被踢下线提示、当前登录用户信息与菜单查询。
 * <p>
 * 类名沿用历史命名 SystemController，容易被误读为"系统设置"模块，但本类实际不涉及站点/系统配置
 * （站点信息配置见 site 包下的 SiteInfoController），职责范围仅限登录、登出、踢人、菜单、当前用户这些
 * 鉴权相关接口；本次改造经确认保留原类名、不做重命名，这里特别说明以免后来者被类名误导。
 * <p>
 * /login（GET）、/kickout 目前仍由服务端 Thymeleaf 模板渲染（templates/system/login.html、kickout.html）：
 * 用户尚未登录时无法进入 /admin 下由 AdminWebController 转发的 Vue SPA 壳，登录页只能独立于 SPA 之外渲染，
 * /kickout 则是全局异常处理在捕获到 Sa-Token 的 NotLoginException（会话被顶掉/被踢下线）后跳转的提示页；
 * 登录成功后，/menu、/currentUser 等接口才是供 Vue admin-app 异步调用获取权限数据的纯 JSON 接口。
 */
@Slf4j
@Controller
@AllArgsConstructor
public class SystemController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final BizCategoryService bizCategoryService;
    private final SysConfigService configService;
    private final LoginAuthenticator loginAuthenticator;

    /**
     * 展示登录页（服务端 Thymeleaf 渲染）。若当前会话已认证，直接重定向到 /admin 首页，不重复展示登录页；
     * 否则附带有效分类列表与系统配置信息，用于登录页顶部导航展示。
     *
     * @param model 视图数据容器
     */
    @GetMapping("/login")
    public ModelAndView login(Model model) {
        ModelAndView modelAndView = new ModelAndView();
        if (StpUtil.isLogin()) {
            modelAndView.setView(new RedirectView("/admin", true, false));
            return modelAndView;
        }
        BizCategory bizCategory = new BizCategory();
        bizCategory.setStatus(CoreConst.STATUS_VALID);
        model.addAttribute("categoryList", bizCategoryService.selectCategories(bizCategory));
        getSysConfig(model);
        modelAndView.setViewName("system/login");
        return modelAndView;
    }

    /**
     * 提交登录：先校验图形验证码（无论校验成功与否都会清除 session 中的验证码，避免被重复提交复用），
     * 再显式完成用户名密码认证；rememberMe 传 1 时使用持久化 Cookie。账号被锁定、用户名或密码错误
     * 会分别返回不同的错误提示；登录成功后更新该用户的最后登录时间。
     *
     * @param request      当前请求，验证码校验需要用到其 session
     * @param username     用户名
     * @param password     明文密码
     * @param verification 图形验证码输入值
     * @param rememberMe   是否记住登录状态，1 表示是，默认 0
     * @return 登录结果
     */
    @PostMapping("/login")
    @ResponseBody
    public ResponseVo login(HttpServletRequest request, String username, String password, String verification,
                            @RequestParam(value = "rememberMe", defaultValue = "0") Integer rememberMe) {
        //判断验证码
        if (!CaptchaJakartaUtil.ver(verification, request)) {
            // 清除session中的验证码
            CaptchaJakartaUtil.clear(request);
            return ResultUtil.error("验证码错误！");
        }
        // 原先这一步交给 Shiro 的 subject.login() 内部完成（查用户 → 校验状态 → 比对密码），
        // Sa-Token 没有 Realm 概念，因此把整个流程显式写在这里，逻辑与原来保持一致。
        User user = userService.selectByUsername(username);
        if (user == null) {
            return ResultUtil.error("用户名或者密码错误！");
        }
        if (CoreConst.STATUS_INVALID.equals(user.getStatus())) {
            return ResultUtil.error("用户已经被锁定不能登录，请联系管理员！");
        }
        if (!loginAuthenticator.authenticate(user, password)) {
            return ResultUtil.error("用户名或者密码错误！");
        }
        // 把登录 IP 放进会话，供在线用户列表展示
        user.setLoginIpAddress(IpUtil.getIpAddr(request));
        UserContext.login(user, 1 == rememberMe);
        //更新最后登录时间
        userService.updateLastLoginTime(user);
        return ResultUtil.success("登录成功！");
    }

    /**
     * 展示"账号已在其他地方登录 / 会话已被顶下线"提示页（服务端 Thymeleaf 渲染），
     * 由全局异常处理在检测到当前会话已失效（被顶下线/被踢下线）后重定向到此。
     *
     * @param model 视图数据容器
     * @return 视图名
     */
    @GetMapping("/kickout")
    public String kickout(Model model) {
        BizCategory bizCategory = new BizCategory();
        bizCategory.setStatus(CoreConst.STATUS_VALID);
        model.addAttribute("categoryList", bizCategoryService.selectCategories(bizCategory));
        getSysConfig(model);
        return "system/kickout";
    }

    /**
     * 登出：清除当前会话的登录态，最后重定向回 /admin（因未认证会被再次引导至登录页）。
     *
     * @return 重定向视图
     */
    @RequestMapping("/logout")
    public ModelAndView logout() {
        // Sa-Token 的 logout() 会自行清理该会话在 Redis 中的登录态与会话数据，
        // 不再需要像 Shiro 那样额外维护"用户名 → 会话队列"的缓存并手工清理。
        UserContext.logout();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setView(new RedirectView("/admin", true, false));
        return modelAndView;
    }

    /**
     * 查询当前登录用户可见的菜单权限列表，供 Vue admin-app 渲染后台左侧导航菜单使用。
     *
     * @return 当前用户的菜单权限列表
     */
    @PostMapping("/menu")
    @ResponseBody
    public List<Permission> getMenus() {
        return permissionService.selectMenuByUserId(UserContext.getCurrentUserId());
    }

    /**
     * 获取当前登录用户信息（后台 Vue SPA 用来做客户端权限按钮展示），
     * perms 复用 findPermsByUserId——和接口鉴权时用的是同一份数据（见 SaTokenPermissionImpl）
     *
     * @return 当前用户名、昵称与权限标识集合
     */
    @PostMapping("/currentUser")
    @ResponseBody
    public ResponseVo<CurrentUserVo> currentUser() {
        User user = UserContext.getCurrentUser();
        Set<String> perms = permissionService.findPermsByUserId(user.getUserId());
        return ResultUtil.success("获取成功", new CurrentUserVo(user.getUsername(), user.getNickname(), perms));
    }

    /**
     * 组装登录/踢出页面渲染所需的公共 Model 数据：系统配置项集合与有效分类列表（用于页面导航展示）。
     */
    private void getSysConfig(Model model) {
        Map<String, String> map = configService.selectAll();
        model.addAttribute("sysConfig", map);
        BizCategory bizCategory = new BizCategory();
        bizCategory.setStatus(CoreConst.STATUS_VALID);
        model.addAttribute("categoryList", bizCategoryService.selectCategories(bizCategory));
    }


}
