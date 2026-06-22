package com.flowmind.user.service;

import com.flowmind.user.dto.RoleRequest;
import com.flowmind.user.entity.SysRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 角色服务接口。
 */
public interface RoleService {

    /**
     * 创建角色。
     */
    SysRole create(RoleRequest request);

    /**
     * 查询角色列表。
     */
    List<SysRole> list();

    /**
     * 分页查询角色。
     */
    Page<SysRole> page(
            String code,
            String name,
            Pageable pageable
    );

    /**
     * 查询角色详情。
     */
    SysRole get(Long id);

    /**
     * 更新角色。
     */
    SysRole update(Long id, RoleRequest request);
}
