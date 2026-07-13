package com.thx.module.gamesave.context;

import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.interceptor.GameDeviceTokenInterceptor;
import lombok.experimental.UtilityClass;

import javax.servlet.http.HttpServletRequest;

/** 从当前 HTTP 请求中解析已认证的 GameSave 调用方上下文。 */
@UtilityClass
public class GameCallerContextResolver {

    public static GameCallerContext resolve(HttpServletRequest request) {
        Object value = request.getAttribute(GameDeviceTokenInterceptor.CALLER_CONTEXT_ATTR);
        if (!(value instanceof GameCallerContext)) {
            throw GameSaveException.unauthorized("UNAUTHENTICATED", "未认证的 GameSave 请求");
        }
        return (GameCallerContext) value;
    }
}
