package com.flowmind.user.dto;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 保存角色菜单授权请求。
 */
@Data
public class RoleMenuRequest {

    /**
     * 勾选的菜单ID集合。
     *
     * 采用覆盖式保存，前端提交当前角色最终拥有的菜单集合。
     */
    private Set<Long> menuIds = new LinkedHashSet<>();
}
