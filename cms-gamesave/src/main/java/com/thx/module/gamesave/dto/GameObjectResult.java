package com.thx.module.gamesave.dto;

import com.thx.module.gamesave.model.GameObject;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 对客户端暴露的内容对象信息，不包含底层 fileId、bucket 或 objectKey。 */
@Data
@AllArgsConstructor
public class GameObjectResult {
    private String objectId;
    private String sha256;
    private long size;

    public static GameObjectResult from(GameObject object) {
        return new GameObjectResult(object.getObjectId(), object.getSha256(), object.getSize());
    }
}
