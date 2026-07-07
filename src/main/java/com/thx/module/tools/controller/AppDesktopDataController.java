package com.thx.module.tools.controller;

import com.thx.module.admin.vo.base.ResponseVo;
import com.thx.module.tools.entity.AppDesktopData;
import com.thx.module.tools.service.AppDesktopDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tools/api/")
public class AppDesktopDataController implements Serializable {

    @Resource
    private AppDesktopDataService appDesktopDataService;

    @GetMapping("/appDesktopData")
    public ResponseVo<List<AppDesktopData>> appDesktopData() {
        List<AppDesktopData> list = appDesktopDataService.list();
        return ResponseVo.success(list);
    }

    @PostMapping("/appDesktopData/add")
    public ResponseVo<AppDesktopData> add(@RequestBody AppDesktopData appDesktopData) {
        boolean save = appDesktopDataService.save(appDesktopData);
        if (save) {
            return ResponseVo.success();
        } else {
            return ResponseVo.error();
        }
    }

    @PostMapping("/appDesktopData/update")
    public ResponseVo<List<AppDesktopData>> update(@RequestBody AppDesktopData appDesktopData) {
        boolean update = appDesktopDataService.updateById(appDesktopData);
        if (update) {
            return ResponseVo.success();
        } else {
            return ResponseVo.error();
        }
    }

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