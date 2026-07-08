package com.thx.module.tools.controller;

import com.thx.module.admin.vo.base.ResponseVo;
import com.thx.module.tools.entity.AppDesktopData;
import com.thx.module.tools.service.AppDesktopDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.List;

/**
 * 应用桌面条目管理接口。
 * 对前台"工具箱/应用桌面"页面展示的应用快捷方式（名称 + 跳转地址 + 图标，见 {@link AppDesktopData}）
 * 提供基础的查询 / 新增 / 更新 / 删除能力，逻辑很薄，直接透传给 {@link AppDesktopDataService}。
 */
@Slf4j
@RestController
@RequestMapping("/tools/api/")
public class AppDesktopDataController implements Serializable {

    @Resource
    private AppDesktopDataService appDesktopDataService;

    /**
     * 查询全部应用桌面条目
     */
    @GetMapping("/appDesktopData")
    public ResponseVo<List<AppDesktopData>> appDesktopData() {
        List<AppDesktopData> list = appDesktopDataService.list();
        return ResponseVo.success(list);
    }

    /**
     * 新增一个应用桌面条目
     */
    @PostMapping("/appDesktopData/add")
    public ResponseVo<AppDesktopData> add(@RequestBody AppDesktopData appDesktopData) {
        boolean save = appDesktopDataService.save(appDesktopData);
        if (save) {
            return ResponseVo.success();
        } else {
            return ResponseVo.error();
        }
    }

    /**
     * 按 id 更新应用桌面条目
     */
    @PostMapping("/appDesktopData/update")
    public ResponseVo<List<AppDesktopData>> update(@RequestBody AppDesktopData appDesktopData) {
        boolean update = appDesktopDataService.updateById(appDesktopData);
        if (update) {
            return ResponseVo.success();
        } else {
            return ResponseVo.error();
        }
    }

    /**
     * 按 id 删除应用桌面条目
     */
    @PostMapping("/appDesktopData/delete")
    public ResponseVo<List<AppDesktopData>> delete(@RequestBody AppDesktopData appDesktopData) {
        boolean remove = appDesktopDataService.removeById(appDesktopData.getId());
        if (remove) {
            return ResponseVo.success();
        } else {
            return ResponseVo.error();
        }
    }

}