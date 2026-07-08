package com.thx.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 基于 JDK 反射（{@link Introspector}）实现的 Bean 属性拷贝工具类。
 * <p>
 * 项目中各模块 DO/VO/DTO 之间的转换大量依赖本类：通过比对源对象与目标对象中
 * 同名的 getter/setter 方法完成属性搬运，源、目标类型之间不要求存在继承关系，
 * 只要属性名相同即可拷贝。
 * <p>
 * 设计上采用"单个属性拷贝失败不影响整体"的宽松容错策略——某个属性因类型不兼容
 * 等原因赋值失败时，只记录日志并继续处理其余属性，不会向上抛出异常，避免因个别
 * 字段问题导致整个对象转换失败。调用方应知悉：拷贝异常是静默的，不会有返回值或
 * 异常提示。
 */
@Slf4j
@UtilityClass
public class CopyUtil {

    /**
     * 将 source 中与 dest 同名的属性值逐一拷贝到 dest（含 null 值，会覆盖 dest 原有值）。
     * 单个属性拷贝失败仅记录日志，不影响其余属性的拷贝，也不会抛出异常。
     *
     * @param source 源对象
     * @param dest   目标对象，属性会被覆盖写入
     */
    public static void copy(Object source, Object dest) {
        try {
            if (source != null || dest != null) {
                // 获取属性
                BeanInfo sourceBean = Introspector.getBeanInfo(source.getClass(), Object.class);
                PropertyDescriptor[] sourceProperty = sourceBean.getPropertyDescriptors();
                BeanInfo destBean = Introspector.getBeanInfo(dest.getClass(), Object.class);
                PropertyDescriptor[] destProperty = destBean.getPropertyDescriptors();
                for (int i = 0; i < sourceProperty.length; i++) {
                    for (int j = 0; j < destProperty.length; j++) {
                        if (sourceProperty[i].getName().equals(destProperty[j].getName())) {
                            try {
                                // 调用source的getter方法和dest的setter方法
                                destProperty[j].getWriteMethod().invoke(dest, sourceProperty[i].getReadMethod().invoke(source));
                                break;
                            } catch (Exception e) {
                                log.info("属性赋值失败," + sourceProperty[i].getName() + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("对象拷贝失败, source: {}, dest: {}", source, dest, e);
        }
    }

    /**
     * 与 {@link #copy(Object, Object)} 类似，但只拷贝 source 中值不为 null 的属性，
     * 避免用 source 里的 null 覆盖掉 dest 已有的值，常用于"部分字段更新"场景。
     *
     * @param source 源对象
     * @param dest   目标对象，仅当 source 对应属性非 null 时才会被覆盖
     */
    public static void copyNotNull(Object source, Object dest) {
        try {
            if (source == null || dest == null) {
                return;
            }
            // 获取属性
            BeanInfo sourceBean = Introspector.getBeanInfo(source.getClass(), Object.class);
            PropertyDescriptor[] sourceProperty = sourceBean.getPropertyDescriptors();
            BeanInfo destBean = Introspector.getBeanInfo(dest.getClass(), Object.class);
            PropertyDescriptor[] destProperty = destBean.getPropertyDescriptors();
            for (int i = 0; i < sourceProperty.length; i++) {
                for (int j = 0; j < destProperty.length; j++) {
                    if (sourceProperty[i].getName().equals(destProperty[j].getName()) && Objects.nonNull(sourceProperty[i].getReadMethod().invoke(source))) {
                        try {
                            // 调用source的getter方法和dest的setter方法
                            destProperty[j].getWriteMethod().invoke(dest, sourceProperty[i].getReadMethod().invoke(source));
                            break;
                        } catch (Exception e) {
                            log.info("属性赋值失败," + sourceProperty[i].getName() + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("对象拷贝失败, source: {}, dest: {}", source, dest, e);
        }
    }

    /**
     * 通过反射创建一个 clazz 的新实例（要求存在无参构造函数），并将 source 的同名属性拷贝进去。
     *
     * @param source 源对象
     * @param clazz  目标类，必须有可访问的无参构造函数
     * @return 拷贝完成的目标类实例；若实例化或拷贝过程抛出异常，返回 null
     */
    public static <T> T getCopy(Object source, Class<T> clazz) {
        T dest = null;
        try {
            dest = clazz.newInstance();
            copy(source, dest);
        } catch (Exception e) {
            log.info("对象复制错误:" + e.getMessage(), e);
        }
        return dest;
    }

    /**
     * 将 source 属性拷贝到已存在的 dest 对象上，可指定需要忽略（不拷贝）的属性名。
     * 注意：与本类其余方法不同，此方法委托给 Spring 的 {@link BeanUtils#copyProperties}
     * 实现（要求属性类型完全一致，不做类型转换），而不是本类自己的反射逻辑。
     *
     * @param source            源对象
     * @param dest              目标对象（已实例化）
     * @param ignoreProperties  需要跳过、不进行拷贝的属性名
     * @return 拷贝后的 dest 对象（同一引用）
     */
    public static Object getCopy(Object source, Object dest, String... ignoreProperties) {
        try {
            BeanUtils.copyProperties(source, dest, ignoreProperties);
        } catch (Exception e) {
            log.info("对象复制错误:" + e.getMessage(), e);
        }
        return dest;
    }

    /**
     * 批量版 {@link #getCopy(Object, Class)}：对 sources 中的每个元素创建一个 clazz 新实例
     * 并拷贝属性，最终返回目标类型的列表。
     *
     * @param sources 源对象集合，为 null 时返回空列表
     * @param clazz   目标类，必须有可访问的无参构造函数
     * @return 拷贝后的目标类型列表；单个元素实例化失败时会跳过该元素并记录日志
     */
    @SuppressWarnings("rawtypes")
    public static <T> List<T> getCopyList(List sources, Class<T> clazz) {
        List<T> clazzs = new ArrayList<>();
        if (sources == null) {
            return clazzs;
        }
        for (Object source : sources) {
            try {
                T dest = clazz.newInstance();
                copy(source, dest);
                clazzs.add(dest);
            } catch (InstantiationException | IllegalAccessException e) {
                log.info("对象复制错误:" + e.getMessage(), e);
            }
        }
        return clazzs;
    }
}