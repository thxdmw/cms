package com.thx.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 工具类
 */
@Slf4j
@UtilityClass
public class MD5 {

    /**
     * 计算字节数组的 MD5 摘要，返回 32 位小写十六进制字符串。
     *
     * @param buffer 待摘要的字节数组
     * @return 32 位小写十六进制 MD5 值；计算异常时返回 null
     */
    public static String getMessageDigest(byte[] buffer) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(buffer);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将字节数组转为十六进制字符串（每个字节固定输出 2 位，不足补 0）。
     *
     * @param array 字节数组
     * @return 十六进制字符串
     */
    public static String hex(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(Integer.toHexString(b & 0xFF | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    /**
     * 计算字符串的 MD5 摘要（先按 CP1252 单字节编码取字节数组，注意这不是 UTF-8，
     * 对非 ASCII 字符会产生编码损失，调用方应仅对 ASCII 内容使用本方法）。
     *
     * @param message 待摘要的字符串
     * @return 十六进制 MD5 值
     */
    public static String md5Hex(String message) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return hex(md.digest(message.getBytes("CP1252")));
    }
}
