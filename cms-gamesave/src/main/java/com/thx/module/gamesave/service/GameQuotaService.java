package com.thx.module.gamesave.service;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameQuotaResult;

/** GameSave 用户级物理存储配额服务。 */
public interface GameQuotaService {

    /** 查询当前用户配额摘要。 */
    GameQuotaResult get(GameCallerContext caller);

    /** 为即将创建的内容对象原子预占容量。 */
    void reserve(String userId, long bytes);

    /** 在内容对象删除或预占失败补偿时释放容量。 */
    void release(String userId, long bytes);
}