package com.flowmind.user.service;

import com.flowmind.user.dto.UserRequest;
import com.flowmind.user.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 用户服务接口。
 */
public interface UserService {

    /**
     * 创建用户。
     */
    SysUser create(UserRequest request);

    /**
     * 更新用户。
     */
    SysUser update(Long id, UserRequest request);

    /**
     * 查询全部用户。
     */
    List<SysUser> list();

    /**
     * 分页查询用户。
     */
    Page<SysUser> page(
            String username,
            String realName,
            String department,
            String status,
            Pageable pageable
    );

    /**
     * 查询单个用户。
     */
    SysUser get(Long id);

    /**
     * 给用户绑定角色。
     */
    SysUser bindRole(Long userId, Long roleId);

    /**
     * 覆盖保存用户角色。
     */
    SysUser updateRoles(Long userId, List<String> roleCodes);
}
