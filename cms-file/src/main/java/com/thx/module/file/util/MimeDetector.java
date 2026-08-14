package com.thx.module.file.util;

import lombok.experimental.UtilityClass;
import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 Apache Tika 的真实文件类型检测
 * 只根据文件内容（magic bytes）判断，不信任 MultipartFile.getContentType()
 */
@UtilityClass
public class MimeDetector {

    /** Tika 门面对象，无状态、线程安全，复用同一个实例即可 */
    private static final Tika TIKA = new Tika();

    /** 检测输入流内容对应的真实 MIME 类型；只读取文件头部字节，不会消费整个流 */
    public static String detect(InputStream inputStream) {
        try {
            return TIKA.detect(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("检测文件类型失败", e);
        }
    }
}
