package com.thx.module.gamesave.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameObjectMapperContractTest {

    @Test
    void reactivatedObjectIsNotImmediatelyCollectedAsOrphan() throws Exception {
        Method reactivate = GameObjectMapper.class.getMethod(
                "reactivateDeleted", Long.class, String.class, String.class);
        String reactivateSql = String.join(" ", reactivate.getAnnotation(Update.class).value()).toLowerCase();
        assertTrue(reactivateSql.contains("update_time = now()"),
                "重新激活必须刷新对象活跃时间");

        Method orphanQuery = Arrays.stream(GameObjectMapper.class.getMethods())
                .filter(method -> method.getName().equals("selectOrphanCandidates"))
                .findFirst().orElseThrow(AssertionError::new);
        String orphanSql = String.join(" ", orphanQuery.getAnnotation(Select.class).value()).toLowerCase();
        assertTrue(orphanSql.contains("update_time <"),
                "孤儿扫描必须按最近上传/重新激活时间判断");
        assertTrue(!orphanSql.contains("create_time <"),
                "孤儿扫描不得继续按最初创建时间判断");

        Method claim = GameObjectMapper.class.getMethod(
                "markOrphanDeleting", String.class, String.class, java.util.Date.class);
        String claimSql = String.join(" ", claim.getAnnotation(Update.class).value()).toLowerCase();
        assertTrue(claimSql.contains("update_time < #{threshold}"),
                "孤儿对象最终抢占必须原子校验活跃时间阈值");

        Method touch = GameObjectMapper.class.getMethod("touchActiveObject", Long.class, String.class);
        String touchSql = String.join(" ", touch.getAnnotation(Update.class).value()).toLowerCase();
        assertTrue(touchSql.contains("update_time = now()"), "复用 ACTIVE 对象必须刷新活跃时间");
    }
}
