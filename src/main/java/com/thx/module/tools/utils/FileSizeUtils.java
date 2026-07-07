// 创建新文件 FileSizeUtils.java
package com.thx.module.tools.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FileSizeUtils {
    
    /**
     * 解析带单位的文件大小字符串为字节数
     * 支持的单位: B, KB, MB, GB
     * @param sizeWithUnit 带单位的大小字符串，如 "10MB", "5KB"
     * @return 字节数
     */
    public static long parseFileSize(String sizeWithUnit) {
        if (sizeWithUnit == null || sizeWithUnit.isEmpty()) {
            throw new IllegalArgumentException("文件大小配置不能为空");
        }
        
        sizeWithUnit = sizeWithUnit.toUpperCase().trim();
        
        // 提取数字部分和单位部分
        StringBuilder numberPart = new StringBuilder();
        StringBuilder unitPart = new StringBuilder();
        
        for (char c : sizeWithUnit.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                numberPart.append(c);
            } else {
                unitPart.append(c);
            }
        }
        
        double number = Double.parseDouble(numberPart.toString());
        String unit = unitPart.toString();
        
        // 转换为字节
        switch (unit) {
            case "":
            case "B":
                return (long) number;
            case "KB":
                return (long) (number * 1024);
            case "MB":
                return (long) (number * 1024 * 1024);
            case "GB":
                return (long) (number * 1024 * 1024 * 1024);
            default:
                throw new IllegalArgumentException("不支持的文件大小单位: " + unit);
        }
    }
}
