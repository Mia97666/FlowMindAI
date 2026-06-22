package com.flowmind.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 系统用户实体。
 *
 * FlowMind AI 的审批人解析、通知投递和发起人归属都依赖用户数据。
 * MVP 阶段先不接入登录态，前端通过 username 选择当前用户。
 */
@Data
@Entity
@Table(name = "sys_user")
public class SysUser {

    /**
     * 用户主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 登录名/系统内唯一账号。
     */
    @Column(unique = true, nullable = false, length = 64)
    private String username;

    /**
     * 用户真实姓名。
     */
    @Column(nullable = false, length = 128)
    private String realName;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 所属部门，MVP 阶段用字符串即可支撑审批人选择和页面展示。
     */
    private String department;

    /**
     * 岗位名称。
     */
    private String position;

    /**
     * 直属领导用户ID。
     *
     * MANAGER 类型审批人会优先根据该字段解析。
     */
    private Long managerId;

    /**
     * 用户状态。
     *
     * ENABLED：可用；DISABLED：停用。
     */
    private String status;

    /**
     * 用户类型。
     *
     * NORMAL：普通用户；AI：系统内置 AI 审批员。
     */
    private String userType;

    /**
     * 用户拥有的角色集合。
     *
     * 使用 EAGER 是为了让前端用户列表可以直接展示角色，
     * 当前数据量较小，不会带来明显性能问题。
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "sys_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<SysRole> roles = new LinkedHashSet<>();

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
