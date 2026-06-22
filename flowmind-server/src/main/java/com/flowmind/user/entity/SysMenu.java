package com.flowmind.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统菜单实体。
 *
 * 菜单既服务于前端页面展示，也服务于 RBAC 权限控制。
 * 当前阶段先做到菜单级权限，后续可以在 permissionCode 上扩展按钮级权限。
 */
@Data
@Entity
@Table(name = "sys_menu")
public class SysMenu {

    /**
     * 菜单主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 父菜单ID。
     *
     * 一级顶部菜单为空，二级左侧菜单指向所属一级菜单或父级分组。
     */
    private Long parentId;

    /**
     * 菜单编码，系统内唯一。
     *
     * 示例：design.form.field。
     */
    @Column(unique = true, nullable = false, length = 128)
    private String menuCode;

    /**
     * 菜单名称。
     */
    @Column(nullable = false, length = 128)
    private String menuName;

    /**
     * 前端路由或页面 key。
     *
     * 原型阶段前端使用该值切换页面；正式路由也可以复用。
     */
    @Column(length = 128)
    private String routeKey;

    /**
     * 菜单类型。
     *
     * TOP：顶部一级导航；
     * GROUP：左侧分组；
     * PAGE：可打开页面。
     */
    @Column(nullable = false, length = 32)
    private String menuType;

    /**
     * 权限编码。
     *
     * 用于后续按钮权限或接口权限扩展。
     */
    @Column(length = 128)
    private String permissionCode;

    /**
     * 排序号，越小越靠前。
     */
    private Integer sortOrder;

    /**
     * 菜单状态。
     *
     * ENABLED：启用；DISABLED：停用。
     */
    @Column(nullable = false, length = 32)
    private String status;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
