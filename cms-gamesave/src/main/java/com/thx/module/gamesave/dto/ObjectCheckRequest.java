package com.thx.module.gamesave.dto;

import lombok.Data;

import java.util.List;

/** 批量检查缺失内容对象请求。 */
@Data
public class ObjectCheckRequest {
    private List<ObjectDescriptor> objects;
}
