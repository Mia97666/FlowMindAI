package com.flowmind.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * 系统角色实体。
 *
 * 这里保持角色模型轻量化：角色只描述身份和权限边界，
 * 具体用户与角色的多对多关系放在 {@link SysUser} 中维护。
 */
@Data
@Entity
@Table(name = "sys_role")
public class SysRole {

    /**
     * 角色主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 角色编码。
     *
     * 示例：ADMIN、FINANCE、MANAGER。
     */
    @Column(unique = true, nullable = false, length = 64)
    private String code;

    /**
     * 角色名称。
     */
    @Column(nullable = false, length = 128)
    private String name;

    /**
     * 角色描述。
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 角色可访问的菜单集合。
     *
     * 这里使用角色到菜单的多对多关系落到 sys_role_menu 表。
     * 为避免用户列表序列化时把菜单权限一并展开，接口层通过 MenuResponse 单独返回菜单树。
     */
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sys_role_menu",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "menu_id")
    )
    private Set<SysMenu> menus = new LinkedHashSet<>();

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
