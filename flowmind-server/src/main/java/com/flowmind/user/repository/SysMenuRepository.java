package com.flowmind.user.repository;

import com.flowmind.user.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 系统菜单数据访问接口。
 */
public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {

    /**
     * 根据菜单编码查询菜单。
     */
    Optional<SysMenu> findByMenuCode(String menuCode);

    /**
     * 查询所有启用菜单并按排序号排列。
     */
    List<SysMenu> findByStatusOrderBySortOrderAscIdAsc(String status);

    /**
     * 查询所有菜单并按排序号排列。
     */
    List<SysMenu> findAllByOrderBySortOrderAscIdAsc();

    /**
     * 批量查询菜单，用于保存角色授权。
     */
    List<SysMenu> findByIdIn(Collection<Long> ids);
}
