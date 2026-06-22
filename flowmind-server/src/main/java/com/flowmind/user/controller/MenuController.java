package com.flowmind.user.controller;

import com.flowmind.user.dto.MenuRequest;
import com.flowmind.user.dto.MenuResponse;
import com.flowmind.user.dto.RoleMenuRequest;
import com.flowmind.user.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 菜单与角色授权接口。
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 查询系统菜单树。
     */
    @GetMapping("/tree")
    public List<MenuResponse> tree() {
        return menuService.tree();
    }

    /**
     * 查询某个用户已授权的系统菜单树。
     */
    @GetMapping("/users/{username}/tree")
    public List<MenuResponse> userTree(@PathVariable String username) {
        return menuService.userTree(username);
    }

    /**
     * 查询全部菜单树，包含停用菜单。
     */
    @GetMapping("/all-tree")
    public List<MenuResponse> allTree() {
        return menuService.allTree();
    }

    /**
     * 查询菜单详情。
     */
    @GetMapping("/{id}")
    public MenuResponse get(@PathVariable Long id) {
        return menuService.get(id);
    }

    /**
     * 创建菜单。
     */
    @PostMapping
    public MenuResponse create(@RequestBody MenuRequest request) {
        return menuService.create(request);
    }

    /**
     * 更新菜单。
     */
    @PutMapping("/{id}")
    public MenuResponse update(
            @PathVariable Long id,
            @RequestBody MenuRequest request
    ) {
        return menuService.update(id, request);
    }

    /**
     * 启用菜单。
     */
    @PostMapping("/{id}/enable")
    public MenuResponse enable(@PathVariable Long id) {
        return menuService.enable(id);
    }

    /**
     * 停用菜单。
     */
    @PostMapping("/{id}/disable")
    public MenuResponse disable(@PathVariable Long id) {
        return menuService.disable(id);
    }

    /**
     * 查询某个角色已授权的菜单ID集合。
     */
    @GetMapping("/roles/{roleId}")
    public Set<Long> roleMenuIds(@PathVariable Long roleId) {
        return menuService.roleMenuIds(roleId);
    }

    /**
     * 覆盖保存某个角色的菜单授权。
     */
    @PutMapping("/roles/{roleId}")
    public Set<Long> saveRoleMenus(
            @PathVariable Long roleId,
            @RequestBody RoleMenuRequest request
    ) {
        return menuService.saveRoleMenus(roleId, request.getMenuIds());
    }
}
