package com.flowmind.user.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户角色授权请求。
 */
@Data
public class UserRoleRequest {

    /**
     * 用户最终拥有的角色编码集合。
     */
    private List<String> roleCodes = new ArrayList<>();
}
