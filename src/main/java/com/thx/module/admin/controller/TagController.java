package com.thx.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.common.util.ResultUtil;
import com.thx.module.admin.entity.BizTags;
import com.thx.module.admin.service.BizTagsService;
import com.thx.module.admin.vo.base.PageResultVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 后台标签配置
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Controller
@RequestMapping("tag")
@AllArgsConstructor
public class TagController {

    private final BizTagsService tagsService;


    @PostMapping("list")
    @ResponseBody
    public PageResultVo loadTags(BizTags bizTags, Integer pageNumber, Integer pageSize) {
        IPage<BizTags> bizTagsPage = tagsService.pageTags(bizTags, pageNumber, pageSize);
        return ResultUtil.table(bizTagsPage.getRecords(), bizTagsPage.getTotal());
    }

    /**
     * 获取全部标签（不分页），供文章新增/编辑表单的标签多选框使用
     */
    @PostMapping("/all")
    @ResponseBody
    public List<BizTags> listAll() {
        return tagsService.list();
    }

    @PostMapping("/add")
    @ResponseBody
    @CacheEvict(value = "tag", allEntries = true)
    public ResponseVo add(BizTags bizTags) {
        Date date = new Date();
        bizTags.setCreateTime(date);
        bizTags.setUpdateTime(date);
        boolean i = tagsService.save(bizTags);
        if (i) {
            return ResultUtil.success("新增标签成功");
        } else {
            return ResultUtil.error("新增标签失败");
        }
    }

    @PostMapping("/edit")
    @ResponseBody
    @CacheEvict(value = "tag", allEntries = true)
    public ResponseVo edit(BizTags bizTags) {
        bizTags.setUpdateTime(new Date());
        boolean i = tagsService.updateById(bizTags);
        if (i) {
            return ResultUtil.success("编辑标签成功");
        } else {
            return ResultUtil.error("编辑标签失败");
        }
    }

    @PostMapping("/delete")
    @ResponseBody
    public ResponseVo delete(String id) {
        return deleteBatch(new String[]{id});
    }

    @PostMapping("/batch/delete")
    @ResponseBody
    public ResponseVo deleteBatch(@RequestParam("ids[]") String[] ids) {
        int i = tagsService.deleteBatch(ids);
        if (i > 0) {
            return ResultUtil.success("删除标签成功");
        } else {
            return ResultUtil.error("删除标签失败");
        }
    }
}
