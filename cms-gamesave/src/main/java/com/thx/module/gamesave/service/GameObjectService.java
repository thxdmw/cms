package com.thx.module.gamesave.service;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.ObjectDescriptor;
import com.thx.module.gamesave.model.GameObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** GameSave 内容对象门面。 */
public interface GameObjectService {
    /**
     * 为当前用户拥有的内容对象申请短时下载地址。
     * 地址由文件模块生成；GameSave 不向客户端暴露任何文件系统应用密钥。
     */
    String getDownloadUrl(String objectId, GameCallerContext caller);

    /** 返回当前用户尚未持有的内容对象，并按 sha256+size 去重。 */
    List<ObjectDescriptor> findMissing(List<ObjectDescriptor> objects, GameCallerContext caller);

    /** 上传或复用内容对象；服务端必须重新校验实际 SHA-256 与大小。 */
    GameObject put(MultipartFile file, String expectedSha256, long expectedSize, GameCallerContext caller);

    /**
     * 快照提交时批量解析当前用户已有内容对象；任意一个不存在都会拒绝整个 Manifest。
     * 返回值以 normalizeHash(sha256) + ":" + size 为 key，供调用方按同样规则回查。
     */
    Map<String, GameObject> requireOwnedObjects(List<ObjectDescriptor> descriptors, GameCallerContext caller);
    /** 释放历史快照对对象的引用；最后一个引用消失时进入文件模块的逻辑删除流程。 */
    void releaseSnapshotReference(String objectId, GameCallerContext caller);
}
