package com.flowmind.user.dto;

import lombok.Data;

/**
 * 菜单保存请求。
 */
@Data
public class MenuRequest {

    private Long parentId;

    private String menuCode;

    private String menuName;

    private String routeKey;

    private String menuType;

    private String permissionCode;

    private Integer sortOrder;

    private String status;
}
