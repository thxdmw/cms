package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.module.admin.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户 Mapper。
 */
public interface UserMapper extends BaseMapper<User> {
    /**
     * 分页查询有效（status=1）用户列表，并关联查出用户所属角色信息；
     * 支持按用户名、邮箱、手机号模糊匹配。
     *
     * @param page 分页参数
     * @param user 查询条件：username、email、phone 均为模糊匹配（LIKE %xxx%），字段为空则不参与过滤
     * @return 分页结果
     */
    IPage<User> selectUsers(@Param("page") IPage<User> page, @Param("user") User user);

    /**
     * 根据角色 id 查询拥有该角色的用户列表（INNER JOIN user_role）。
     *
     * @param roleId 角色 id
     * @return 用户列表
     */
    List<User> findByRoleId(String roleId);

    /**
     * 根据角色 id 集合查询拥有其中任一角色的用户列表（INNER JOIN user_role，roleId IN 集合）。
     *
     * @param roleIds 角色 id 集合
     * @return 用户列表
     */
    List<User> findByRoleIds(List<String> roleIds);
}
