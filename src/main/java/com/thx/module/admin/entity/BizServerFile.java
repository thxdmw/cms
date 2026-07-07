package com.thx.module.admin.entity;

import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 服务器文件管理（业务元数据，文件内容托管在 file 模块）
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2026年7月6日
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class BizServerFile extends BaseVo {
    private static final long serialVersionUID = 1L;

    /** 关联 file 模块的 file_id */
    private String fileId;
    private String originalName;
    private String extension;
    private String contentType;
    private Long size;
    /** 是否可在线预览/编辑的文本类型：1是 0否 */
    private Integer editable;
    /** 上传人昵称（冗余展示） */
    private String uploader;
    private String uploadUserId;
    private String remark;

}
