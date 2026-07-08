package com.thx.common.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 分页对象，在 MyBatis-Plus {@link Page} 的基础上扩展了页面展示常用的分页信息
 * （总页数、上/下一页页码、是否首末页等），避免前端每次都要基于 current/pages 自行推算。
 * <p>
 * 这些扩展字段并非在构造时就有效，而是依赖 {@link #setTotal(long)} 被调用后才会
 * 被重新计算——MyBatis-Plus 分页插件在查询完成后会调用 setTotal 回填总记录数，
 * 因此本类重写该方法，"顺便"把衍生字段一起算好，调用方无需手动维护。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Pagination<T> extends Page<T> {

    private static final long serialVersionUID = 5194933845448697148L;

    public Pagination(long current, long size) {
        super(current, size);
    }

    //总页数
    private long pages;

    //前一页
    private long prePage;
    //下一页
    private long nextPage;

    //是否为第一页
    private boolean isFirstPage;
    //是否为最后一页
    private boolean isLastPage;
    //是否有前一页
    private boolean hasPreviousPage;
    //是否有下一页
    private boolean hasNextPage;

    /**
     * 重写父类 setTotal：在设置总记录数的同时，联动重新计算总页数、上/下一页页码、
     * 是否首末页等衍生字段，由 MyBatis-Plus 分页插件在查询完成后自动回调。
     *
     * @param total 总记录数
     * @return 当前分页对象本身，便于链式调用
     */
    @Override
    public Pagination<T> setTotal(long total) {
        super.setTotal(total);
        pages = super.getPages();
        long current = getCurrent();
        isFirstPage = current == 1;
        isLastPage = current == pages || pages == 0;
        hasPreviousPage = current > 1;
        hasNextPage = current < pages;
        if (current > 1) {
            prePage = current - 1;
        }
        if (current < pages) {
            nextPage = current + 1;
        }
        return this;
    }
}