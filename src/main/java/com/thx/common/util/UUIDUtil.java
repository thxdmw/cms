package com.thx.common.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;

/**
 * 生成UUID工具类，提供标准 UUID、定长数字唯一 ID、8 位短 UUID 三种不同形态的
 * 唯一标识生成方式，按业务场景（如是否需要固定长度、是否要求可读性更短）选用。
 */
@UtilityClass
public class UUIDUtil {

    /** {@link #generateShortUuid()} 生成的短 UUID 长度。 */
    private static final int SHORT_LENGTH = 8;

    /**
     * 生成标准 UUID 并去掉中间的 "-" 分隔符，得到 32 位十六进制字符串。
     *
     * @return 32 位无分隔符的 UUID 字符串
     */
    public static String uuid() {
        String str = UUID.randomUUID().toString();
        return str.replace("-", "");
    }

    /**
     * 生成一个以机器编号开头、后接定长数字哈希的唯一 ID，形如 "1000000000123456"。
     * 数字部分基于随机 UUID 的 hashCode 取绝对值后按 15 位补零，machineId 目前写死为 1，
     * 预留了个位数（1-9）区分集群多机部署的能力，但当前尚未接入真实的机器编号配置。
     *
     * @return 唯一 ID 字符串
     */
    public static String getUniqueIdByUUId() {
        //最大支持1-9个集群机器部署
        int machineId = 1;
        int hashCode = UUID.randomUUID().toString().hashCode();
        if (hashCode < 0) {
            hashCode = -hashCode;
        }
        // 0 代表前面补充0
        // 4 代表长度为4
        // d 代表参数为正数型
        return machineId + String.format("%015d", hashCode);
    }

    /**
     * 本地手工验证用的临时入口：打印一次 {@link #getUniqueIdByUUId()} 和 {@link #uuid()}
     * 的生成结果，便于开发时肉眼确认格式，非业务调用入口。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.out.println(getUniqueIdByUUId());
        System.out.println(uuid());
    }


    /** {@link #generateShortUuid()} 使用的 62 进制字符表（0-9、a-z、A-Z）。 */
    private static final String[] CHARS = {"a", "b", "c", "d", "e", "f",
            "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
            "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z"};


    /**
     * 生成 8 位短 UUID：将标准 UUID 的 32 位十六进制串每 4 位一组转成整数，
     * 再对 62（0x3E）取模映射到 {@link #CHARS} 字符表，得到一个更短、仍然
     * 具备较低碰撞概率的可读标识符。
     *
     * @return 8 位短 UUID 字符串
     */
    public static String generateShortUuid() {
        StringBuilder shortBuffer = new StringBuilder();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < SHORT_LENGTH; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            shortBuffer.append(CHARS[x % 0x3E]);
        }
        return shortBuffer.toString();

    }


}
