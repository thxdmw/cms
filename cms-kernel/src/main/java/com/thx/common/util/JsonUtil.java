package com.thx.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * 通用 JSON 工具类，基于 Spring Boot 自带的 Jackson 实现。
 * <p>
 * 项目原先在若干处直接使用 fastjson 的 {@code JSON}/{@code JSONObject} 做手写序列化，
 * 已统一收敛到本类，以便去掉 fastjson 依赖（历史 CVE 较多），同时与 Spring MVC
 * 默认的 Jackson 序列化行为保持一致。
 * <p>
 * 注意：这里刻意使用独立的 {@link ObjectMapper} 实例，<b>不能</b>复用
 * {@code RedisConfig} 中那个开启了 {@code activateDefaultTyping} 的 mapper——
 * 后者会在 JSON 中额外写入 {@code @class} 类型信息，用于 Redis 反序列化还原对象，
 * 直接用于 HTTP 响应会污染报文结构。
 */
@UtilityClass
public class JsonUtil {

    /**
     * 反序列化时忽略 JSON 中存在但目标类型没有的字段，避免上游（如 Python OCR 服务）
     * 新增返回字段就导致解析失败。
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 待序列化对象，可为 null（返回字符串 "null"）
     * @return JSON 字符串
     * @throws IllegalArgumentException 序列化失败时抛出
     */
    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 序列化失败: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * 把 JSON 字符串解析为指定类型的对象。
     *
     * @param json JSON 字符串
     * @param type 目标类型
     * @return 解析后的对象
     * @throws IllegalArgumentException 解析失败时抛出
     */
    public static <T> T parse(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 解析失败: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * 把 JSON 数组字符串解析为元素类型为 {@code elementType} 的 List。
     *
     * @param json        JSON 数组字符串
     * @param elementType 数组元素类型
     * @return 解析后的 List
     * @throws IllegalArgumentException 解析失败时抛出
     */
    public static <T> List<T> parseList(String json, Class<T> elementType) {
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 数组解析失败: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * 把 JSON 字符串解析为树模型，适用于字段结构不固定、只需按需取值的场景
     * （替代 fastjson 的 {@code JSON.parseObject(str)} 返回 JSONObject 的用法）。
     *
     * @param json JSON 字符串
     * @return JSON 树根节点
     * @throws IllegalArgumentException 解析失败时抛出
     */
    public static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 解析失败: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * 把 JSON 数组节点转换为元素类型为 {@code elementType} 的 List，
     * 替代 fastjson 中 {@code JSONArray.toJavaList(Xxx.class)} 的用法。
     * 节点为 null、JSON null 或非数组时返回空 List。
     *
     * @param node        JSON 数组节点
     * @param elementType 数组元素类型
     * @return 转换后的 List，永不为 null
     */
    public static <T> List<T> convertList(JsonNode node, Class<T> elementType) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        return MAPPER.convertValue(node,
                MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
    }

    /**
     * 读取对象节点下某个字段的字符串值，字段缺失或为 JSON null 时返回 null。
     * <p>
     * 不能直接用 {@code node.path(field).asText()}：Jackson 中缺失字段会返回空串、
     * JSON null 会返回字符串 {@code "null"}，与 fastjson {@code getString()} 返回
     * {@code null} 的语义不一致，迁移时容易引入隐蔽 bug。
     *
     * @param node  对象节点
     * @param field 字段名
     * @return 字段字符串值，缺失或为 null 时返回 null
     */
    public static String getString(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    /**
     * 把 JSON 树节点转换为指定类型，供 {@link #readTree(String)} 取到子节点后继续转换。
     *
     * @param node JSON 节点，可为 null（返回 null）
     * @param type 目标类型
     * @return 转换后的对象
     * @throws IllegalArgumentException 转换失败时抛出
     */
    public static <T> T convert(JsonNode node, Class<T> type) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return MAPPER.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 节点转换失败: " + e.getOriginalMessage(), e);
        }
    }
}
