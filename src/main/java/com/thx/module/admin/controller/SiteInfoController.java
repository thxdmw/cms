package com.thx.module.admin.controller;

import com.thx.common.util.ResultUtil;
import com.thx.module.admin.service.SysConfigService;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 后台网站信息配置
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Controller
@AllArgsConstructor
public class SiteInfoController {

    private final SysConfigService configService;

    @GetMapping("/siteinfo/detail")
    @ResponseBody
    public ResponseVo<Map<String, String>> detail() {
        return ResultUtil.success("获取成功", configService.selectAll());
    }

    @PostMapping("/siteinfo/edit")
    @ResponseBody
    public ResponseVo save(@RequestParam Map<String, String> map, HttpServletRequest request, HttpServletResponse response) {
        try {
            configService.updateAll(map, request, response);
            return ResultUtil.success("保存网站信息成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.error("保存网站信息失败");
        }
    }
}
