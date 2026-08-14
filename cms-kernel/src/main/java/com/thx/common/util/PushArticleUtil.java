package com.thx.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 文章推送到百度工具类，用于调用百度搜索资源平台的"链接提交"接口（主动推送），
 * 将站内新增/更新的文章 URL 主动上报给百度，加快收录速度。
 */
@Slf4j
@UtilityClass
public class PushArticleUtil {

    /**
     * 调用百度主动推送接口，将 parameters 中的文章 URL 列表以纯文本、每行一条的形式
     * POST 给 postUrl（百度分配的带 token 的专属推送地址）。
     * <p>
     * 注意：建立连接阶段的 {@link IOException} 只记录日志、不会中断方法执行，若发生该异常
     * 会导致后续 {@code conn} 为 null，在写请求体时抛出 NPE 并被下方 try-with-resources
     * 所在的 catch 捕获，最终返回空字符串而非 null。
     *
     * @param postUrl    百度推送接口地址（含 token）
     * @param parameters 待推送的文章 URL 数组
     * @return 百度接口返回的响应内容；参数非法返回 null，请求过程异常返回空字符串
     */
    public static String postBaidu(String postUrl, String[] parameters) {
        if (null == postUrl || null == parameters || parameters.length == 0) {
            return null;
        }
        StringBuilder result = new StringBuilder();

        //建立URL之间的连接
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(postUrl).openConnection();
            //设置通用的请求属性
            conn.setRequestProperty("Host", "data.zz.baidu.com");
            conn.setRequestProperty("User-Agent", "curl/7.12.1");
            conn.setRequestProperty("Content-Length", "83");
            conn.setRequestProperty("Content-Type", "text/plain");
            conn.setRequestMethod("POST");
            //发送POST请求必须设置如下两行
            conn.setDoInput(true);
            conn.setDoOutput(true);
        } catch (IOException e) {
            log.error("【推送百度】建立URL之间的连接失败:{}", e.getMessage(), e);
        }
        try (PrintWriter out = new PrintWriter(conn.getOutputStream())) {

            //发送请求参数
            StringBuilder param = new StringBuilder();
            for (String s : parameters) {
                param.append(s).append('\n');
            }
            out.print(param.toString().trim());
            //进行输出流的缓冲
            out.flush();
            //通过BufferedReader输入流来读取Url的响应
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            log.error("推送百度出现异常:{}", e.getMessage(), e);
        }
        return result.toString();
    }
}
