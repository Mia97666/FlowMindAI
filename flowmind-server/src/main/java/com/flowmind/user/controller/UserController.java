package com.flowmind.user.controller;

import com.flowmind.user.dto.UserRequest;
import com.flowmind.user.dto.UserRoleRequest;
import com.flowmind.user.entity.SysUser;
import com.flowmind.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理接口。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 创建用户。
     */
    @PostMapping
    public SysUser create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    /**
     * 查询用户列表。
     */
    @GetMapping
    public List<SysUser> list() {
        return userService.list();
    }

    /**
     * 分页查询用户。
     */
    @GetMapping("/page")
    public Page<SysUser> page(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return userService.page(
                username,
                realName,
                department,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        );
    }

    /**
     * 查询用户详情。
     */
    @GetMapping("/{id}")
    public SysUser get(@PathVariable Long id) {
        return userService.get(id);
    }

    /**
     * 更新用户。
     */
    @PutMapping("/{id}")
    public SysUser update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request
    ) {
        return userService.update(id, request);
    }

    /**
     * 给用户绑定角色。
     */
    @PostMapping("/{userId}/roles/{roleId}")
    public SysUser bindRole(
            @PathVariable Long userId,
            @PathVariable Long roleId
    ) {
        return userService.bindRole(userId, roleId);
    }

    /**
     * 覆盖保存用户角色。
     */
    @PutMapping("/{userId}/roles")
    public SysUser updateRoles(
            @PathVariable Long userId,
            @RequestBody UserRoleRequest request
    ) {
        return userService.updateRoles(userId, request.getRoleCodes());
    }
}
