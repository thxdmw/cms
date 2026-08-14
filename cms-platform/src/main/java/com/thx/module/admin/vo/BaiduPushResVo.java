package com.thx.module.admin.vo;

/**
 * 百度链接主动推送接口（{@link com.thx.enums.SysConfigKey#BAIDU_PUSH_URL} 配置的地址）的响应结果，
 * 字段名和百度接口原始 JSON 返回字段保持一致，供 {@link com.thx.common.util.PushArticleUtil} 反序列化使用。
 */
public class BaiduPushResVo {
    private int success;    //成功推送的url条数
    private int remain;     //当天剩余的可推送url条数
    private String[] not_same_site; //由于不是本站url而未处理的url列表
    private String[] not_valid;     //不合法的url列表

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getRemain() {
        return remain;
    }

    public void setRemain(int remain) {
        this.remain = remain;
    }

    public String[] getNotSameSite() {
        return not_same_site;
    }

    public void setNotSameSite(String[] not_same_site) {
        this.not_same_site = not_same_site;
    }

    public String[] getNotValid() {
        return not_valid;
    }

    public void setNotValid(String[] not_valid) {
        this.not_valid = not_valid;
    }
}
