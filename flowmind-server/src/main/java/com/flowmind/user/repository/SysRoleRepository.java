package com.flowmind.user.repository;

import com.flowmind.user.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 角色数据访问接口。
 */
public interface SysRoleRepository extends JpaRepository<SysRole, Long>, JpaSpecificationExecutor<SysRole> {

    /**
     * 根据角色编码查询角色。
     */
    Optional<SysRole> findByCode(String code);
}
