package com.thx.common.shiro;

import com.thx.common.config.properties.FileUploadProperties;
import com.thx.common.config.properties.StaticizeProperties;
import com.thx.common.util.CoreConst;
import com.thx.module.admin.service.PermissionService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Shiro 过滤链与 GameSave 设备 Token 认证边界测试。 */
class ShiroServiceTest {

    @Test
    void gameSaveApiMustBypassShiroSessionFilter() {
        PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.selectAll(CoreConst.STATUS_VALID)).thenReturn(Collections.emptyList());

        FileUploadProperties fileUploadProperties = mock(FileUploadProperties.class);
        when(fileUploadProperties.getAccessPathPattern()).thenReturn("/upload/**");
        StaticizeProperties staticizeProperties = mock(StaticizeProperties.class);
        when(staticizeProperties.getAccessPathPattern()).thenReturn("/static/**");

        ShiroService service = new ShiroService(
                permissionService,
                null,
                fileUploadProperties,
                staticizeProperties);

        Map<String, String> definitions = service.loadFilterChainDefinitions();

        assertEquals("anon", definitions.get("/api/game-save/v1/**"));
    }
}