package com.flowmind.user.service;

import com.flowmind.user.dto.MenuResponse;
import com.flowmind.user.dto.MenuRequest;

import java.util.List;
import java.util.Set;

/**
 * 菜单与角色授权服务。
 */
public interface MenuService {

    /**
     * 查询系统启用菜单树。
     */
    List<MenuResponse> tree();

    /**
     * 查询用户已授权的启用菜单树。
     */
    List<MenuResponse> userTree(String username);

    /**
     * 查询全部菜单树，包含停用菜单。
     */
    List<MenuResponse> allTree();

    /**
     * 查询菜单详情。
     */
    MenuResponse get(Long id);

    /**
     * 创建菜单。
     */
    MenuResponse create(MenuRequest request);

    /**
     * 更新菜单。
     */
    MenuResponse update(Long id, MenuRequest request);

    /**
     * 启用菜单。
     */
    MenuResponse enable(Long id);

    /**
     * 停用菜单。
     */
    MenuResponse disable(Long id);

    /**
     * 查询角色已授权菜单ID。
     */
    Set<Long> roleMenuIds(Long roleId);

    /**
     * 覆盖保存角色菜单授权。
     */
    Set<Long> saveRoleMenus(Long roleId, Set<Long> menuIds);
}
