package com.flowmind.user.repository;

import com.flowmind.user.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口。
 */
public interface SysUserRepository extends JpaRepository<SysUser, Long>, JpaSpecificationExecutor<SysUser> {

    /**
     * 根据用户名查询用户。
     */
    Optional<SysUser> findByUsername(String username);

    /**
     * 按角色查找启用用户。
     *
     * 该方法服务于 ROLE 类型审批人解析：例如 FINANCE 角色自动落到财务人员。
     */
    List<SysUser> findByRoles_CodeAndStatusOrderByIdAsc(String roleCode, String status);

    /**
     * 按部门和角色查找启用用户。
     *
     * 该方法服务于部门负责人审批。
     */
    List<SysUser> findByDepartmentAndRoles_CodeAndStatusOrderByIdAsc(
            String department,
            String roleCode,
            String status
    );
}
