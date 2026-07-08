package com.thx.common.util;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * 日期工具类，集中了项目中用到的日期格式常量、日期解析/格式化、日期加减、
 * 日期区间计算等静态方法，是使用频率最高的基础设施类之一。
 * <p>
 * 方法命名沿用了较早期的风格（如 webFormat、newFormat 等常量并未严格遵循
 * 全大写命名规范），这是历史遗留，为避免影响所有调用方，本次改造未做重命名，
 * 仅补充注释说明每个方法的实际格式/语义。
 * <p>
 * 使用时需注意两类方法的差异：方法名包含 "parse" 的用于把字符串解析成
 * {@link Date}，包含 "get..String"/"format" 的用于把 {@link Date} 格式化成字符串；
 * 大部分 parse 系列方法内部设置了 {@code setLenient(false)}，即不允许"溢出"日期
 * （如 2 月 30 日）被自动纠正为 3 月 2 日，而是直接抛出 {@link ParseException}。
 */
@Slf4j
@UtilityClass
public class DateUtil {

    /** 一天的秒数 */
    public static final long ONE_DAY_SECONDS = 86400;
    /** 短日期格式：yyyyMMdd */
    public static final String SHORT_FORMAT = "yyyyMMdd";
    /** 长日期时间格式：yyyyMMddHHmmss */
    public static final String LONG_FORMAT = "yyyyMMddHHmmss";
    /** 带毫秒的紧凑格式：yyyyMMddHHmmssSSS，常用于生成时间戳类唯一编号 */
    public static final String concurrentFormat = "yyyyMMddHHmmssSSS";
    /** 两位年份+毫秒的紧凑格式：yyMMddHHmmssSSS */
    public static final String shortConcurrentFormat = "yyMMddHHmmssSSS";
    /** 页面展示用日期格式：yyyy-MM-dd */
    public static final String webFormat = "yyyy-MM-dd";
    /** 页面展示用年月格式：yyyy-MM */
    public static final String webMonthFormat = "yyyy-MM";
    /** 仅时间格式：HH:mm:ss */
    public static final String timeFormat = "HH:mm:ss";
    /** 年月格式：yyyyMM */
    public static final String monthFormat = "yyyyMM";
    /** 中文日期格式：yyyy年MM月dd日 */
    public static final String chineseDtFormat = "yyyy年MM月dd日";
    /** 中文年月格式：yyyy年MM月 */
    public static final String chineseYMFormat = "yyyy年MM月";
    /** 页面展示用日期时间格式：yyyy-MM-dd HH:mm:ss */
    public static final String newFormat = "yyyy-MM-dd HH:mm:ss";
    /** 不含秒的日期时间格式：yyyy-MM-dd HH:mm */
    public static final String noSecondFormat = "yyyy-MM-dd HH:mm";
    /** 月-日格式：MM-dd */
    public static final String MdFormat = "MM-dd";
    /** 一天的毫秒数 */
    public static final long ONE_DAY_MILL_SECONDS = 86400000;

    /**
     * 创建一个非宽松模式（{@code setLenient(false)}）的 {@link DateFormat}，
     * 避免把不合法日期（如 2 月 30 日）自动纠正为合法日期而不报错。
     *
     * @param pattern 日期格式
     * @return 非宽松模式的 DateFormat
     */
    public static DateFormat getNewDateFormat(String pattern) {
        DateFormat df = new SimpleDateFormat(pattern);
        df.setLenient(false);
        return df;
    }

    /**
     * 按指定格式将日期格式化为字符串。
     *
     * @param date   日期
     * @param format 目标格式
     * @return 格式化后的字符串
     */
    public static String format(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 将字符串日期从旧格式解析后，重新格式化为新格式字符串。
     *
     * @param dateStr   原始日期字符串
     * @param oldFormat 原始格式
     * @param newFormat 目标格式
     * @return 转换后的日期字符串
     */
    public static String format(String dateStr, String oldFormat, String newFormat) throws ParseException {
        String result = null;
        DateFormat oldDateFormat = new SimpleDateFormat(oldFormat);
        DateFormat newDateFormat = new SimpleDateFormat(newFormat);
        Date date = oldDateFormat.parse(dateStr);
        result = newDateFormat.format(date);
        return result;
    }

    /**
     * 按 {@link #SHORT_FORMAT}（yyyyMMdd）解析日期字符串。
     *
     * @param sDate 待解析的日期字符串
     * @return 解析后的日期
     */
    public static Date parseDateNoTime(String sDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(SHORT_FORMAT);
        return dateFormat.parse(sDate);
    }

    /**
     * 按指定格式解析日期字符串。
     *
     * @param sDate  待解析的日期字符串
     * @param format 日期格式，不能为空
     * @return 解析后的日期
     * @throws ParseException format 为空，或字符串不符合格式时抛出
     */
    public static Date parseDateNoTime(String sDate, String format) throws ParseException {
        if (StrUtil.isBlank(format)) {
            throw new ParseException("Null format. ", 0);
        }

        DateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.parse(sDate);
    }

    /**
     * 先按正则 delimit 去除日期字符串中的分隔符，再按 {@link #SHORT_FORMAT}（yyyyMMdd）解析。
     *
     * @param sDate   待解析的日期字符串，如 "2019-09-11"
     * @param delimit 分隔符正则，如 "-"
     * @return 解析后的日期
     */
    public static Date parseDateNoTimeWithDelimit(String sDate, String delimit) throws ParseException {
        sDate = sDate.replaceAll(delimit, "");
        DateFormat dateFormat = new SimpleDateFormat(SHORT_FORMAT);
        return dateFormat.parse(sDate);
    }

    /**
     * 按 {@link #LONG_FORMAT}（yyyyMMddHHmmss）解析日期字符串。
     *
     * @param sDate 待解析的日期字符串
     * @return 解析后的日期
     */
    public static Date parseDateLongFormat(String sDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(LONG_FORMAT);
        return dateFormat.parse(sDate);
    }

    /**
     * 按 {@link #newFormat}（yyyy-MM-dd HH:mm:ss）以非宽松模式解析日期字符串。
     *
     * @param sDate 待解析的日期字符串
     * @return 解析后的日期
     */
    public static Date parseDateNewFormat(String sDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(newFormat);
        dateFormat.setLenient(false);
        return dateFormat.parse(sDate);
    }

    /**
     * 按 {@link #noSecondFormat}（yyyy-MM-dd HH:mm）以非宽松模式解析日期字符串。
     *
     * @param sDate 待解析的日期字符串
     * @return 解析后的日期
     */
    public static Date parseDateNoSecondFormat(String sDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(noSecondFormat);
        dateFormat.setLenient(false);
        return dateFormat.parse(sDate);
    }

    /**
     * 按 {@link #webFormat}（yyyy-MM-dd）以非宽松模式解析日期字符串。
     *
     * @param sDate 待解析的日期字符串
     * @return 解析后的日期
     */
    public static Date parseDateWebFormat(String sDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(webFormat);
        dateFormat.setLenient(false);
        return dateFormat.parse(sDate);
    }

    /**
     * 按 {@link #webMonthFormat}（yyyy-MM）以非宽松模式解析日期字符串。
     *
     * @param sDate 待解析的日期字符串
     * @return 解析后的日期
     */
    public static Date parseDateWebMonthFormat(String sDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(webMonthFormat);
        dateFormat.setLenient(false);
        return dateFormat.parse(sDate);
    }

    /**
     * 计算当前时间几小时之后的时间
     *
     * @param date
     * @param hours
     * @return
     */
    public static Date addHours(Date date, long hours) {
        return addMinutes(date, hours * 60);
    }

    /**
     * 计算当前时间几分钟之后的时间
     *
     * @param date
     * @param minutes
     * @return
     */
    public static Date addMinutes(Date date, long minutes) {
        return addSeconds(date, minutes * 60);
    }

    /**
     * 计算指定时间几秒之后的时间，secs 为负数时表示往前推。
     *
     * @param date1
     * @param secs
     * @return
     */

    public static Date addSeconds(Date date1, long secs) {
        return new Date(date1.getTime() + secs * 1000);
    }

    /**
     * 判断输入的字符串是否为合法的小时
     *
     * @param hourStr
     * @return true/false
     */
    public static boolean isValidHour(String hourStr) {
        if (NumberUtil.isNumber(hourStr)) {
            int hour = Integer.parseInt(hourStr);
            return hour >= 0 && hour <= 23;
        }
        return false;
    }

    /**
     * 判断输入的字符串是否为合法的分或秒
     *
     * @param str
     * @return true/false
     */
    public static boolean isValidMinuteOrSecond(String str) {
        if (NumberUtil.isNumber(str)) {
            int hour = Integer.parseInt(str);
            return hour >= 0 && hour <= 59;
        }
        return false;
    }

    /**
     * 取得新的日期
     *
     * @param date1 日期
     * @param days  天数
     * @return 新的日期
     */
    public static Date addDays(Date date1, long days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date1);
        cal.add(Calendar.DATE, (int) days);
        return cal.getTime();
    }

    /**
     * 取得指定日期（{@link #SHORT_FORMAT} 格式字符串）的明天日期，返回同样格式的字符串。
     *
     * @param sDate yyyyMMdd 格式的日期字符串
     * @return 明天日期，yyyyMMdd 格式
     */
    public static String getTomorrowDateString(String sDate) throws ParseException {
        Date aDate = parseDateNoTime(sDate);

        aDate = addSeconds(aDate, ONE_DAY_SECONDS);

        return getDateString(aDate);
    }

    /**
     * 取得指定日期（{@link #webFormat} 格式字符串）的明天日期，返回同样格式的字符串。
     *
     * @param sDate yyyy-MM-dd 格式的日期字符串
     * @return 明天日期，yyyy-MM-dd 格式
     */
    public static String getTomorrowDateNewFMTString(String sDate) throws ParseException {
        Date aDate = parseDateWebFormat(sDate);
        aDate = addDays(aDate, 1);
        return getWebDateString(aDate);
    }

    /**
     * 取得指定日期（{@link #newFormat} 格式字符串）的明天日期，返回 {@link #webFormat} 格式字符串。
     *
     * @param sDate yyyy-MM-dd HH:mm:ss 格式的日期字符串
     * @return 明天日期，yyyy-MM-dd 格式
     */
    public static String getTomorrowDateNewFormatString(String sDate) throws ParseException {
        Date aDate = parseDateNewFormat(sDate);
        aDate = addDays(aDate, 1);
        return getWebDateString(aDate);
    }

    /**
     * 将日期格式化为 {@link #LONG_FORMAT}（yyyyMMddHHmmss）字符串。
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String getLongDateString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat(LONG_FORMAT);

        return getDateString(date, dateFormat);
    }

    /**
     * 将日期格式化为 {@link #newFormat}（yyyy-MM-dd HH:mm:ss）字符串。
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String getNewFormatDateString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat(newFormat);
        return getDateString(date, dateFormat);
    }

    /**
     * 将日期格式化为 {@link #webFormat}（yyyy-MM-dd）字符串。
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String getWebFormatDateString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat(webFormat);
        return getDateString(date, dateFormat);
    }

    /**
     * 将日期格式化为 {@link #concurrentFormat}（yyyyMMddHHmmssSSS，精确到毫秒）字符串，
     * 常用于生成带时间戳的业务编号。
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String getConcurrentFormatDateString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat(concurrentFormat);
        return getDateString(date, dateFormat);
    }

    /**
     * 使用指定 DateFormat 格式化日期。
     *
     * @param date       日期
     * @param dateFormat 格式化器
     * @return 格式化后的字符串；date 或 dateFormat 为 null 时返回 null
     */
    public static String getDateString(Date date, DateFormat dateFormat) {
        if (date == null || dateFormat == null) {
            return null;
        }

        return dateFormat.format(date);
    }

    /**
     * 取得指定日期（{@link #SHORT_FORMAT} 格式字符串）的昨天日期，返回同样格式的字符串。
     *
     * @param sDate yyyyMMdd 格式的日期字符串
     * @return 昨天日期，yyyyMMdd 格式
     */
    public static String getYesterDayDateString(String sDate) throws ParseException {
        Date aDate = parseDateNoTime(sDate);

        aDate = addSeconds(aDate, -ONE_DAY_SECONDS);

        return getDateString(aDate);
    }

    /**
     * 将指定日期格式化为 {@link #SHORT_FORMAT}（yyyyMMdd）字符串。
     *
     * @param date 日期
     * @return 格式化后的字符串，形如 "yyyyMMdd"
     */
    public static String getDateString(Date date) {
        DateFormat df = getNewDateFormat(SHORT_FORMAT);

        return df.format(date);
    }

    /**
     * 将日期格式化为 {@link #webFormat}（yyyy-MM-dd）字符串。效果与
     * {@link #getWebFormatDateString(Date)} 相同（格式化场景下宽松/非宽松模式没有区别），
     * 属于历史遗留的重复实现，保留是为了不影响既有调用方。
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String getWebDateString(Date date) {
        DateFormat dateFormat = getNewDateFormat(webFormat);

        return getDateString(date, dateFormat);
    }

    /**
     * 取得"X年X月X日"的日期格式
     *
     * @param date 日期
     * @return 格式化后的字符串，如 "2019年09月11日"
     */
    public static String getChineseDateString(Date date) {
        DateFormat dateFormat = getNewDateFormat(chineseDtFormat);

        return getDateString(date, dateFormat);
    }

    /**
     * 取得当天日期，格式化为 {@link #SHORT_FORMAT}（yyyyMMdd）字符串。
     *
     * @return 当天日期字符串
     */
    public static String getTodayString() {
        DateFormat dateFormat = getNewDateFormat(SHORT_FORMAT);

        return getDateString(new Date(), dateFormat);
    }

    /**
     * 取得明天日期，格式化为 {@link #SHORT_FORMAT}（yyyyMMdd）字符串。
     *
     * @return 明天日期字符串
     */
    public static String getTomorrowString() {
        DateFormat dateFormat = getNewDateFormat(SHORT_FORMAT);

        return getDateString(DateUtil.addDays(new Date(), 1), dateFormat);
    }

    /**
     * 将日期格式化为 {@link #timeFormat}（HH:mm:ss）字符串，仅保留时分秒部分。
     *
     * @param date 日期
     * @return 格式化后的字符串
     */
    public static String getTimeString(Date date) {
        DateFormat dateFormat = getNewDateFormat(timeFormat);

        return getDateString(date, dateFormat);
    }

    /**
     * 取得当前时间往前推 days 天的日期，格式化为 {@link #SHORT_FORMAT}（yyyyMMdd）字符串。
     *
     * @param days 往前推的天数
     * @return 日期字符串
     */
    public static String getBeforeDayString(int days) {
        Date date = new Date(System.currentTimeMillis() - ONE_DAY_MILL_SECONDS * days);
        DateFormat dateFormat = getNewDateFormat(SHORT_FORMAT);

        return getDateString(date, dateFormat);
    }

    /**
     * 取得两个日期间隔毫秒数（日期1-日期2）
     *
     * @param one 日期1
     * @param two 日期2
     * @return 间隔秒数
     */
    public static long getDiffMillis(Date one, Date two) {
        Calendar sysDate = new GregorianCalendar();

        sysDate.setTime(one);

        Calendar failDate = new GregorianCalendar();

        failDate.setTime(two);
        return sysDate.getTimeInMillis() - failDate.getTimeInMillis();
    }

    /**
     * 取得两个日期间隔秒数（日期1-日期2）
     *
     * @param one 日期1
     * @param two 日期2
     * @return 间隔秒数
     */
    public static long getDiffSeconds(Date one, Date two) {
        Calendar sysDate = new GregorianCalendar();

        sysDate.setTime(one);

        Calendar failDate = new GregorianCalendar();

        failDate.setTime(two);
        return (sysDate.getTimeInMillis() - failDate.getTimeInMillis()) / 1000;
    }

    /**
     * 取得两个日期间隔分钟数（日期1-日期2）
     *
     * @param one 日期1
     * @param two 日期2
     * @return 间隔秒数
     */
    public static long getDiffMinutes(Date one, Date two) {
        Calendar sysDate = new GregorianCalendar();

        sysDate.setTime(one);

        Calendar failDate = new GregorianCalendar();

        failDate.setTime(two);
        return (sysDate.getTimeInMillis() - failDate.getTimeInMillis()) / (60 * 1000);
    }

    /**
     * 取得两个日期的间隔天数（日期1-日期2），按毫秒差整除得到，
     * 不足 24 小时的部分会被舍去（例如相差 23 小时也会算作 0 天）。
     * 如果需要按日历上的自然日计算，应使用 {@link #getDiffNaturalDays(Date, Date)}。
     *
     * @param one
     * @param two
     * @return 间隔天数
     */
    public static long getDiffDays(Date one, Date two) {
        Calendar sysDate = new GregorianCalendar();

        sysDate.setTime(one);

        Calendar failDate = new GregorianCalendar();

        failDate.setTime(two);
        return (sysDate.getTimeInMillis() - failDate.getTimeInMillis()) / (24 * 60 * 60 * 1000);
    }

    /**
     * 取得两个日期相差的自然日（按日历天数计算，忽略时分秒，与 {@link #getDiffDays} 的
     * 按毫秒整除方式不同，取绝对值，恒为非负数）。
     *
     * @param date1
     * @param date2
     * @return 相差的自然日天数
     */
    public static long getDiffNaturalDays(Date date1, Date date2) throws ParseException {
        return Math.abs(getDiffNaturalDayNotAbs(date1, date2));
    }

    /**
     * 取得两个日期相差的自然日（按日历天数计算，忽略时分秒），不取绝对值，
     * date1 早于 date2 时结果为负数。
     *
     * @param date1
     * @param date2
     * @return 相差的自然日天数，date1 早于 date2 时为负
     */
    public static long getDiffNaturalDayNotAbs(Date date1, Date date2) throws ParseException {

        long diffDays;
        DateFormat dateFormat = new SimpleDateFormat(webFormat);

        //去掉时分秒
        String dateStr1 = dateFormat.format(date1);
        String dateStr2 = dateFormat.format(date2);
        diffDays = (dateFormat.parse(dateStr1).getTime() - dateFormat.parse(dateStr2).getTime()) / (24 * 60 * 60 * 1000);

        return diffDays;
    }

    /**
     * 取得指定日期（{@link #SHORT_FORMAT} 格式字符串）往前推 days 天的日期字符串。
     *
     * @param dateString yyyyMMdd 格式的日期字符串
     * @param days       往前推的天数
     * @return 日期字符串，yyyyMMdd 格式
     */
    public static String getBeforeDayString(String dateString, int days) throws ParseException {
        DateFormat df = getNewDateFormat(SHORT_FORMAT);
        Date date = df.parse(dateString);
        date = new Date(date.getTime() - ONE_DAY_MILL_SECONDS * days);

        return df.format(date);
    }

    /**
     * 校验字符串是否为合法的 {@link #SHORT_FORMAT}（yyyyMMdd）格式日期：
     * 依次校验长度、是否纯数字、能否被正确解析。
     *
     * @param strDate 待校验的字符串
     * @return 合法返回 true，否则返回 false
     */
    public static boolean isValidShortDateFormat(String strDate) {
        if (strDate == null || strDate.length() != SHORT_FORMAT.length()) {
            return false;
        }

        try {
            // ---- 避免日期中输入非数字 ----
            Integer.parseInt(strDate);
        } catch (NumberFormatException e) {
            return false;
        }

        DateFormat df = getNewDateFormat(SHORT_FORMAT);

        try {
            df.parse(strDate);
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    /**
     * 先按 delimiter 正则去除日期字符串中的分隔符，再校验是否为合法的
     * {@link #SHORT_FORMAT}（yyyyMMdd）格式。
     *
     * @param strDate   待校验的字符串
     * @param delimiter 分隔符正则
     * @return 合法返回 true，否则返回 false
     */
    public static boolean isValidShortDateFormat(String strDate, String delimiter) {
        String temp = strDate.replaceAll(delimiter, "");

        return isValidShortDateFormat(temp);
    }

    /**
     * 判断表示时间的字符是否为符合yyyyMMddHHmmss格式
     *
     * @param strDate
     * @return
     */
    public static boolean isValidLongDateFormat(String strDate) {
        if (strDate.length() != LONG_FORMAT.length()) {
            return false;
        }

        try {
            Long.parseLong(strDate); // ---- 避免日期中输入非数字 ----
        } catch (Exception NumberFormatException) {
            return false;
        }

        DateFormat df = getNewDateFormat(LONG_FORMAT);

        try {
            df.parse(strDate);
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    /**
     * 判断表示时间的字符是否为符合yyyyMMddHHmmss格式
     *
     * @param strDate
     * @param delimiter
     * @return
     */
    public static boolean isValidLongDateFormat(String strDate, String delimiter) {
        String temp = strDate.replaceAll(delimiter, "");

        return isValidLongDateFormat(temp);
    }

    /**
     * 按默认分隔符（"-" 或 "/"）去除后转换为 {@link #SHORT_FORMAT}（yyyyMMdd）格式。
     *
     * @param strDate 日期字符串，如 "2019-09-11" 或 "2019/09/11"
     * @return 转换后的字符串；非法日期返回 null
     */
    public static String getShortDateString(String strDate) {
        return getShortDateString(strDate, "-|/");
    }

    /**
     * 按 delimiter 指定的分隔符正则去除后转换为 {@link #SHORT_FORMAT}（yyyyMMdd）格式。
     *
     * @param strDate   日期字符串
     * @param delimiter 分隔符正则
     * @return 转换后的字符串；strDate 为空或非法日期时返回 null
     */
    public static String getShortDateString(String strDate, String delimiter) {
        if (StrUtil.isBlank(strDate)) {
            return null;
        }

        String temp = strDate.replaceAll(delimiter, "");

        if (isValidShortDateFormat(temp)) {
            return temp;
        }

        return null;
    }

    /**
     * 取得当月第一天，格式化为 {@link #SHORT_FORMAT}（yyyyMMdd）字符串。
     *
     * @return 当月第一天的日期字符串
     */
    public static String getShortFirstDayOfMonth() {
        Calendar cal = Calendar.getInstance();
        Date dt = new Date();

        cal.setTime(dt);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        DateFormat df = getNewDateFormat(SHORT_FORMAT);

        return df.format(cal.getTime());
    }

    /**
     * 取得当天日期，格式化为 {@link #webFormat}（yyyy-MM-dd）字符串。
     *
     * @return 当天日期字符串
     */
    public static String getWebTodayString() {
        DateFormat df = getNewDateFormat(webFormat);

        return df.format(new Date());
    }

    /**
     * 获取当月首日
     *
     * @return 当月首日，yyyy-MM-dd 格式字符串
     */
    public static String getWebFirstDayOfMonth() {
        Calendar cal = Calendar.getInstance();
        Date dt = new Date();

        cal.setTime(dt);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        DateFormat df = getNewDateFormat(webFormat);

        return df.format(cal.getTime());
    }

    /**
     * 获取当月的总天数
     *
     * @return 当前系统时间所在月份的总天数
     */
    public static int getDaysOfMonth() {
        Calendar cal = Calendar.getInstance(Locale.CHINA);
        return cal.getActualMaximum(Calendar.DATE);
    }

    /**
     * 将字符串日期从 formatIn 格式转换为 formatOut 格式。
     *
     * @param dateString 原始日期字符串
     * @param formatIn   原始格式
     * @param formatOut  目标格式
     * @return 转换后的字符串；解析失败时返回空字符串（不抛异常）
     */
    public static String convert(String dateString, DateFormat formatIn, DateFormat formatOut) {
        try {
            Date date = formatIn.parse(dateString);

            return formatOut.format(date);
        } catch (ParseException e) {
            return "";
        }
    }

    /**
     * 将 {@link #SHORT_FORMAT}（yyyyMMdd）格式字符串转换为 {@link #webFormat}（yyyy-MM-dd）格式。
     *
     * @param dateString yyyyMMdd 格式字符串
     * @return yyyy-MM-dd 格式字符串
     */
    public static String convert2WebFormat(String dateString) {
        DateFormat df1 = getNewDateFormat(SHORT_FORMAT);
        DateFormat df2 = getNewDateFormat(webFormat);

        return convert(dateString, df1, df2);
    }

    /**
     * 将 {@link #SHORT_FORMAT}（yyyyMMdd）格式字符串转换为 {@link #chineseDtFormat}（X年X月X日）格式。
     *
     * @param dateString yyyyMMdd 格式字符串
     * @return "X年X月X日" 格式字符串
     */
    public static String convert2ChineseDtFormat(String dateString) {
        DateFormat df1 = getNewDateFormat(SHORT_FORMAT);
        DateFormat df2 = getNewDateFormat(chineseDtFormat);

        return convert(dateString, df1, df2);
    }

    /**
     * 将 {@link #webFormat}（yyyy-MM-dd）格式字符串转换为 {@link #SHORT_FORMAT}（yyyyMMdd）格式。
     *
     * @param dateString yyyy-MM-dd 格式字符串
     * @return yyyyMMdd 格式字符串
     */
    public static String convertFromWebFormat(String dateString) {
        DateFormat df1 = getNewDateFormat(SHORT_FORMAT);
        DateFormat df2 = getNewDateFormat(webFormat);

        return convert(dateString, df2, df1);
    }

    /**
     * 判断 date1 是否不早于 date2（即 date1 &gt;= date2），两者均为 {@link #webFormat}（yyyy-MM-dd）格式字符串。
     *
     * @param date1 日期字符串1
     * @param date2 日期字符串2
     * @return date1 不早于 date2 返回 true
     */
    public static boolean webDateNotLessThan(String date1, String date2) {
        DateFormat df = getNewDateFormat(webFormat);

        return dateNotLessThan(date1, date2, df);
    }

    /**
     * 按 format 解析后比较两个日期字符串，判断 date1 是否不早于 date2。
     *
     * @param date1  日期字符串1
     * @param date2  日期字符串2
     * @param format 解析格式
     * @return date1 不早于 date2 返回 true；解析失败按 false 处理
     */
    public static boolean dateNotLessThan(String date1, String date2, DateFormat format) {
        try {
            Date d1 = format.parse(date1);
            Date d2 = format.parse(date2);

            return !d1.before(d2);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * 格式化为邮件正文中常用的中文日期时间描述，如 "2019年09月11日12:00:00"。
     *
     * @param today 日期
     * @return 格式化后的字符串
     */
    public static String getEmailDate(Date today) {
        String todayStr;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH:mm:ss");

        todayStr = sdf.format(today);
        return todayStr;
    }

    /**
     * 格式化为短信正文中常用的日期时间描述，如 "09月11日12:00"。
     *
     * @param today 日期
     * @return 格式化后的字符串
     */
    public static String getSmsDate(Date today) {
        String todayStr;
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日HH:mm");

        todayStr = sdf.format(today);
        return todayStr;
    }

    /**
     * 将日期格式化为 {@link #monthFormat}（yyyyMM）字符串。
     *
     * @param date 日期
     * @return 格式化后的字符串；date 为 null 时返回 null
     */
    public static String formatMonth(Date date) {
        if (date == null) {
            return null;
        }

        return new SimpleDateFormat(monthFormat).format(date);
    }

    /**
     * 获取系统日期的前一天日期，返回Date
     *
     * @return 前一天日期（含当前时刻的时分秒，只是整体减去一天的毫秒数）
     */
    public static Date getBeforeDate() {
        Date date = new Date();

        return new Date(date.getTime() - ONE_DAY_MILL_SECONDS);
    }

    /**
     * 获得指定时间当天起点时间（即当天 00:00:00）。内部实现是先把日期格式化为
     * yyyyMMdd 字符串，再反解析回 Date，从而丢弃时分秒部分；若中途解析异常（理论上不会发生），
     * 则原样返回传入的 date。
     *
     * @param date 日期
     * @return 当天零点的日期
     */
    public static Date getDayBegin(Date date) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        df.setLenient(false);

        String dateString = df.format(date);

        try {
            return df.parse(dateString);
        } catch (ParseException e) {
            return date;
        }
    }

    /**
     * 根据Date对象返回今天是星期几
     *
     * @param date
     * @return 1:星期日 2:星期一 3:星期二 4:星期三 5:星期四 6:星期五 7:星期六
     */
    public static int getWeekDayFromDateEntity(Date date) {

        Calendar calendar = Calendar.getInstance();// 获得一个日历
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_WEEK);

    }

    /**
     * 判断参date上min分钟后，是否小于当前时间
     *
     * @param date
     * @param min
     * @return
     */
    public static boolean dateLessThanNowAddMin(Date date, long min) {
        return addMinutes(date, min).before(new Date());

    }

    /**
     * 判断日期是否早于当前时间。
     *
     * @param date 日期
     * @return date 为 null 时返回 false；否则判断是否早于当前时间
     */
    public static boolean isBeforeNow(Date date) {
        if (date == null) {
            return false;
        }
        return date.compareTo(new Date()) < 0;
    }

    /**
     * 获得当前月的开始日期
     *
     * @param date {@link #webFormat}（yyyy-MM-dd）格式日期字符串
     * @return 该日期所在月份的第一天
     */
    public static Date getMinMonthDate(String date) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat fmt = new SimpleDateFormat(webFormat);
        calendar.setTime(fmt.parse(date));
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        return calendar.getTime();
    }

    /**
     * 获得当前月的结束日期
     *
     * @param date {@link #webFormat}（yyyy-MM-dd）格式日期字符串
     * @return 该日期所在月份的最后一天
     */
    public static Date getMaxMonthDate(String date) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat fmt = new SimpleDateFormat(webFormat);
        calendar.setTime(fmt.parse(date));
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar.getTime();
    }

    /**
     * 按 {@link #noSecondFormat}（yyyy-MM-dd HH:mm）解析日期字符串，宽松模式
     * （与 {@link #parseDateNoSecondFormat(String)} 的区别是未调用 {@code setLenient(false)}）。
     *
     * @param sDate 待解析的日期字符串
     * @return 解析后的日期
     */
    public static Date parseNoSecondFormat(String sDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(noSecondFormat);
        return dateFormat.parse(sDate);
    }

    /**
     * date日期转变成指定格式字符串
     *
     * @param date         日期
     * @param time_pattern 目标格式
     * @return 格式化后的字符串
     */
    public static String convertDate2String(Date date, String time_pattern) {
        SimpleDateFormat sf = new SimpleDateFormat(time_pattern);
        return sf.format(date);

    }

    /**
     * 根据Date对象返回天
     *
     * @param date
     */
    public static int getDayFromDateEntity(Date date) {
        Calendar calendar = Calendar.getInstance();// 获得一个日历
        calendar.setTime(date);
        return calendar.get(Calendar.DATE);

    }

    /**
     * 按 {@link #webMonthFormat}（yyyy-MM）解析两个字符串后比较先后。
     *
     * @param dateStr        日期字符串1
     * @param anotherDateStr 日期字符串2
     * @return 与 {@link Long#compare(long, long)} 语义相同：小于、等于、大于分别返回负数、0、正数
     */
    public static int compareDateStr(String dateStr, String anotherDateStr) throws ParseException {
        DateFormat df = new SimpleDateFormat(webMonthFormat);
        Date dt1 = df.parse(dateStr);
        Date dt2 = df.parse(anotherDateStr);
        return Long.compare(dt1.getTime(), dt2.getTime());
    }

    /**
     * 取得当前月份，格式化为 {@link #webMonthFormat}（yyyy-MM）字符串。
     *
     * @return 当前月份字符串
     */
    public static String getCurMonth() {
        return format(new Date(), webMonthFormat);
    }

    /**
     * 将 {@link #chineseYMFormat}（X年X月）格式字符串解析后再格式化输出，
     * 用于校验/规整输入格式（合法输入原样返回，非法输入将抛出 ParseException）。
     *
     * @param date "X年X月" 格式字符串
     * @return 规整后的 "X年X月" 格式字符串
     */
    public static String getChineseYMString(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(chineseYMFormat);
        Date datea = sdf.parse(date);
        DateFormat dateFormat = getNewDateFormat(chineseYMFormat);
        return getDateString(datea, dateFormat);
    }

    /**
     * 取得指定月份（{@link #webMonthFormat}，yyyy-MM 格式字符串）的上一个自然月对应的日期。
     *
     * @param date yyyy-MM 格式字符串
     * @return 上一个月对应的日期
     */
    public static Date getPreMonthDate(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(webMonthFormat);
        Date datea = sdf.parse(date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(datea);
        cal.add(Calendar.MONTH, -1);
        return cal.getTime();
    }

    /**
     * 取得指定月份（{@link #webMonthFormat}，yyyy-MM 格式字符串）的下一个自然月对应的日期。
     *
     * @param date yyyy-MM 格式字符串
     * @return 下一个月对应的日期
     */
    public static Date getNextMonthDate(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(webMonthFormat);
        Date datea = sdf.parse(date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(datea);
        cal.add(Calendar.MONTH, 1);
        return cal.getTime();
    }

    /**
     * 获取指定日期的当月的第一天
     *
     * @param date
     * @return 当月第一天，yyyy-MM-dd 格式字符串
     */
    public static String getAssignedDateFirstDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        DateFormat df = getNewDateFormat(webFormat);
        return df.format(cal.getTime());
    }

    /**
     * 获取指定日期的当月的最后一天
     *
     * @param date
     * @return 当月最后一天，yyyy-MM-dd 格式字符串
     */
    public static String getAssignedDateLastDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        DateFormat df = getNewDateFormat(webFormat);
        return df.format(cal.getTime());
    }

    /**
     * 取得指定日期的次日（加一天，时分秒保持不变）。
     *
     * @param date 日期
     * @return 次日日期
     */
    public static Date getNextDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return new Date(calendar.getTimeInMillis());
    }

    /**
     * 根据年 月 获取对应的月份 天数。month 从 1 开始计数（1=一月）。
     * 实现技巧：先把日历定位到该月 1 日，再通过 {@link Calendar#roll} 回退一天，
     * 从而落在该月最后一天，读取其"日"字段即为该月总天数。
     */
    public static int getDaysByYearMonth(int year, int month) {
        Calendar a = Calendar.getInstance();
        a.set(Calendar.YEAR, year);
        a.set(Calendar.MONTH, month - 1);
        a.set(Calendar.DATE, 1);
        a.roll(Calendar.DATE, -1);
        return a.get(Calendar.DATE);
    }

}
