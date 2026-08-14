package com.thx.common.util;

import lombok.experimental.UtilityClass;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则工具类
 */
@UtilityClass
public class RegexUtils {

    /**
     * 找出 str 中所有匹配 regex 的子串。
     *
     * @param str   要匹配的字符串
     * @param regex 正则表达式字符串
     * @return 所有匹配到的子串列表（可能为空列表）；str 为 null 时返回 null
     */
    public static List<String> match(String str, String regex) {
        if (null == str) {
            return null;
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        List<String> list = new LinkedList<>();
        while (matcher.find()) {
            list.add(matcher.group());
        }
        return list;
    }

    /**
     * 判断 str 中是否存在与 regex 匹配的子串（非要求整串匹配，只要能找到一处即可）。
     *
     * @param str   要匹配的字符串
     * @param regex 正则表达式字符串
     * @return str 为 null 返回 false；否则返回是否存在匹配
     */
    public static boolean checkByRegex(String str, String regex) {
        if (null == str) {
            return false;
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }
}

