package com.flowmind.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建或更新用户请求。
 */
@Data
public class UserRequest {

    /**
     * 用户名，系统内唯一。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    /**
     * 真实姓名。
     */
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;

    /**
     * 手机号。
     */
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    /**
     * 邮箱。
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    /**
     * 部门。
     */
    @Size(max = 100, message = "部门长度不能超过100个字符")
    private String department;

    /**
     * 岗位。
     */
    @Size(max = 100, message = "岗位长度不能超过100个字符")
    private String position;

    /**
     * 直属领导ID。
     */
    private Long managerId;

    /**
     * 用户类型，NORMAL 或 AI。
     */
    private String userType;

    /**
     * 用户状态，ENABLED 或 DISABLED。
     */
    private String status;

    /**
     * 角色编码列表。
     */
    private List<String> roleCodes;
}