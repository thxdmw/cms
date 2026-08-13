package com.thx.common.util;

import lombok.experimental.UtilityClass;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * HTML 清洗工具，用于把用户提交的内容（评论昵称、正文等）中的 HTML 标签清除，防止存储型 XSS。
 * <p>
 * 原先使用 hutool 的 {@code HtmlUtil.filter()}，为了把依赖从 {@code hutool-all} 收敛到
 * {@code hutool-core}（{@code HtmlUtil} 属于 {@code hutool-http} 模块），改为使用项目中
 * 已有的 jsoup 实现。jsoup 是 HTML 清洗的业界标准方案，基于真实 HTML 解析而非正则匹配，
 * 对畸形标签、编码绕过等场景的防护强于原实现。
 * <p>
 * <b>行为差异</b>：jsoup 在移除标签的同时，会把 {@code & < > "} 等字符做 HTML 实体转义
 * （例如 {@code &} 会存成 {@code &amp;}）。这是有意保留的——转义后的内容无论前端用
 * 文本还是 HTML 方式渲染都不会被当作标签执行，比"仅去标签不转义"更安全。
 */
@UtilityClass
public class HtmlSanitizer {

    /** 不保留任何标签，同时关闭 jsoup 的美化换行，避免长文本被插入额外空白。 */
    private static final Safelist NO_TAGS = Safelist.none();
    private static final Document.OutputSettings OUTPUT_SETTINGS =
            new Document.OutputSettings().prettyPrint(false);

    /**
     * 清除文本中的全部 HTML 标签，并对特殊字符做实体转义。
     *
     * @param html 用户提交的原始内容，可为 null
     * @return 清洗后的安全文本；入参为 null 时返回 null
     */
    public static String filter(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, "", NO_TAGS, OUTPUT_SETTINGS);
    }
}
