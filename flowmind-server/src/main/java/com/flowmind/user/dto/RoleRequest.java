package com.flowmind.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建或更新角色请求。
 */
@Data
public class RoleRequest {

    /**
     * 角色编码。
     */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50个字符")
    private String code;

    /**
     * 角色名称。
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称长度不能超过100个字符")
    private String name;

    /**
     * 角色描述。
     */
    @Size(max = 200, message = "角色描述长度不能超过200个字符")
    private String description;
}