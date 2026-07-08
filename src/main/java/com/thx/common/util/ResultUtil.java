package com.thx.common.util;

import com.thx.enums.ResponseStatus;
import com.thx.module.admin.vo.base.PageResultVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * 返回结果封装工具类，是 {@link ResponseVo} 静态工厂方法之外的另一套等价调用习惯
 * （项目里两种写法并存：admin 侧多用 ResultUtil.xxx()，blog/agent 侧多用 ResponseVo.xxx()），
 * 状态码统一取自 {@link ResponseStatus}，与 ResponseVo 保持同一数据源。
 */
@UtilityClass
public class ResultUtil {

    public static ResponseVo success() {
        return vo(ResponseStatus.SUCCESS.getCode(), null, null);
    }

    public static ResponseVo success(String msg) {
        return vo(ResponseStatus.SUCCESS.getCode(), msg, null);
    }

    public static ResponseVo success(String msg, Object data) {
        return vo(ResponseStatus.SUCCESS.getCode(), msg, data);
    }

    public static ResponseVo error() {
        return vo(ResponseStatus.ERROR.getCode(), null, null);
    }

    public static ResponseVo error(String msg) {
        return vo(ResponseStatus.ERROR.getCode(), msg, null);
    }

    public static ResponseVo error(String msg, Object data) {
        return vo(ResponseStatus.ERROR.getCode(), msg, data);
    }

    public static PageResultVo table(List<?> list, Long total) {
        return new PageResultVo(list, total);
    }

    public static ResponseVo vo(Integer status, String message, Object data) {
        return new ResponseVo<>(status, message, data);
    }


}
