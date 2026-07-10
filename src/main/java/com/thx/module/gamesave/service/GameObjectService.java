package com.thx.module.gamesave.service;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.ObjectDescriptor;
import com.thx.module.gamesave.model.GameObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** GameSave 内容对象门面。 */
public interface GameObjectService {

    /** 返回当前用户尚未持有的内容对象。 */
    List<ObjectDescriptor> findMissing(List<ObjectDescriptor> objects, GameCallerContext caller);

    /** 上传或复用内容对象；服务端必须重新校验实际 SHA-256 与大小。 */
    GameObject put(MultipartFile file, String expectedSha256, long expectedSize, GameCallerContext caller);
}
