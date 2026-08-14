package com.thx.module.gamesave.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 客户端与服务端统一使用 SHA-256 + size 描述内容对象。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectDescriptor {
    private String sha256;
    private long size;
}
