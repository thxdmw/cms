package com.thx.module.admin.util;

import lombok.experimental.UtilityClass;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 把远程 URL（如 MinIO 预签名下载地址）的内容原样代理转发给客户端。
 * 用途：让浏览器始终从同源接口拿到文件流并按"下载"处理，而不是直接跳转到第三方预签名链接
 * （跳转会让浏览器按 MinIO 返回的 Content-Type 决定预览还是下载，且暴露了带签名的直链）。
 */
@UtilityClass
public class HttpStreamProxyUtil {

    /**
     * @param remoteUrl   实际文件所在的远程地址（如预签名 URL）
     * @param response    当前请求的响应对象，方法内部直接写入响应体
     * @param fileName    下载时浏览器另存为的文件名
     * @param contentType 响应的 Content-Type，为空时回退为 application/octet-stream
     * @param size        文件大小（用于 Content-Length），未知时可传 null
     */
    public static void proxyDownload(String remoteUrl, HttpServletResponse response, String fileName, String contentType, Long size) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(remoteUrl).openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(60_000);

            response.setContentType(contentType != null ? contentType : "application/octet-stream");
            if (size != null) {
                response.setHeader("Content-Length", String.valueOf(size));
            }
            response.setHeader("Content-Disposition", buildAttachmentHeader(fileName));

            try (InputStream in = connection.getInputStream();
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
        } catch (Exception e) {
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 组装同时兼容旧浏览器（ASCII fallback）和现代浏览器（UTF-8 filename*）的 Content-Disposition 头
     */
    private static String buildAttachmentHeader(String fileName) throws Exception {
        String fallback = fileName
                .replaceAll("[\\/:*?\"<>|\r\n]+", "_")
                .replaceAll("[^\\x20-\\x7E]+", "_");
        if (fallback.trim().isEmpty()) {
            fallback = "download";
        }
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        return "attachment; filename=\"" + fallback + "\"; filename*=UTF-8''" + encoded;
    }
}
