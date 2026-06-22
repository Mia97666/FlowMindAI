package com.flowmind.user.controller;

import com.flowmind.user.dto.RoleRequest;
import com.flowmind.user.entity.SysRole;
import com.flowmind.user.service.RoleService;
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
 * 角色管理接口。
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 创建角色。
     */
    @PostMapping
    public SysRole create(@Valid @RequestBody RoleRequest request) {
        return roleService.create(request);
    }

    /**
     * 查询角色列表。
     */
    @GetMapping
    public List<SysRole> list() {
        return roleService.list();
    }

    /**
     * 分页查询角色。
     */
    @GetMapping("/page")
    public Page<SysRole> page(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return roleService.page(
                code,
                name,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        );
    }

    /**
     * 查询角色详情。
     */
    @GetMapping("/{id}")
    public SysRole get(@PathVariable Long id) {
        return roleService.get(id);
    }

    /**
     * 更新角色。
     */
    @PutMapping("/{id}")
    public SysRole update(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request
    ) {
        return roleService.update(id, request);
    }
}
