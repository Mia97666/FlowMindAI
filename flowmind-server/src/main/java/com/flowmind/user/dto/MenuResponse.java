package com.flowmind.user.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树响应对象。
 *
 * 前端顶部导航和左侧导航可以直接使用该结构渲染。
 */
@Data
public class MenuResponse {

    /**
     * 菜单ID。
     */
    private Long id;

    /**
     * 父菜单ID。
     */
    private Long parentId;

    /**
     * 菜单编码。
     */
    private String menuCode;

    /**
     * 菜单名称。
     */
    private String menuName;

    /**
     * 前端页面 key。
     */
    private String routeKey;

    /**
     * 菜单类型。
     */
    private String menuType;

    /**
     * 权限编码。
     */
    private String permissionCode;

    /**
     * 排序号。
     */
    private Integer sortOrder;

    /**
     * 菜单状态。
     */
    private String status;

    /**
     * 子菜单。
     */
    private List<MenuResponse> children = new ArrayList<>();
}
