package com.flowmind.user.service.impl;

import com.flowmind.user.dto.MenuRequest;
import com.flowmind.user.dto.MenuResponse;
import com.flowmind.user.entity.SysMenu;
import com.flowmind.user.entity.SysRole;
import com.flowmind.user.entity.SysUser;
import com.flowmind.user.repository.SysMenuRepository;
import com.flowmind.user.repository.SysRoleRepository;
import com.flowmind.user.repository.SysUserRepository;
import com.flowmind.user.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单与角色授权服务实现。
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private static final String ENABLED = "ENABLED";

    private final SysMenuRepository menuRepository;

    private final SysRoleRepository roleRepository;

    private final SysUserRepository userRepository;

    @Override
    public List<MenuResponse> tree() {
        List<SysMenu> menus = menuRepository.findByStatusOrderBySortOrderAscIdAsc(ENABLED);
        return buildTree(menus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> userTree(String username) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + username));
        List<SysMenu> menus = user.getRoles().stream()
                .flatMap(role -> role.getMenus().stream())
                .filter(menu -> ENABLED.equals(menu.getStatus()))
                .sorted(Comparator
                        .comparing((SysMenu menu) -> menu.getSortOrder() == null ? 999 : menu.getSortOrder())
                        .thenComparing(SysMenu::getId))
                .collect(Collectors.toCollection(ArrayList::new));
        return buildTree(menus);
    }

    @Override
    public List<MenuResponse> allTree() {
        List<SysMenu> menus = menuRepository.findAllByOrderBySortOrderAscIdAsc();
        return buildTree(menus);
    }

    @Override
    public MenuResponse get(Long id) {
        return toResponse(getMenu(id));
    }

    @Override
    @Transactional
    public MenuResponse create(MenuRequest request) {
        validateRequest(request);
        menuRepository.findByMenuCode(request.getMenuCode()).ifPresent(menu -> {
            throw new IllegalArgumentException("菜单编码已存在：" + menu.getMenuCode());
        });

        SysMenu menu = new SysMenu();
        fillMenu(menu, request);
        menu.setCreatedAt(LocalDateTime.now());
        menu.setUpdatedAt(LocalDateTime.now());
        return toResponse(menuRepository.save(menu));
    }

    @Override
    @Transactional
    public MenuResponse update(Long id, MenuRequest request) {
        validateRequest(request);
        SysMenu menu = getMenu(id);
        menuRepository.findByMenuCode(request.getMenuCode())
                .filter(existed -> !existed.getId().equals(id))
                .ifPresent(existed -> {
                    throw new IllegalArgumentException("菜单编码已存在：" + existed.getMenuCode());
                });
        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new IllegalArgumentException("父菜单不能选择自己");
        }
        fillMenu(menu, request);
        menu.setUpdatedAt(LocalDateTime.now());
        return toResponse(menuRepository.save(menu));
    }

    @Override
    @Transactional
    public MenuResponse enable(Long id) {
        SysMenu menu = getMenu(id);
        menu.setStatus(ENABLED);
        menu.setUpdatedAt(LocalDateTime.now());
        return toResponse(menuRepository.save(menu));
    }

    @Override
    @Transactional
    public MenuResponse disable(Long id) {
        SysMenu menu = getMenu(id);
        menu.setStatus("DISABLED");
        menu.setUpdatedAt(LocalDateTime.now());
        return toResponse(menuRepository.save(menu));
    }

    private List<MenuResponse> buildTree(List<SysMenu> menus) {
        Map<Long, MenuResponse> responseMap = new LinkedHashMap<>();

        for (SysMenu menu : menus) {
            responseMap.put(menu.getId(), toResponse(menu));
        }

        List<MenuResponse> roots = new ArrayList<>();
        for (MenuResponse menu : responseMap.values()) {
            if (menu.getParentId() == null || !responseMap.containsKey(menu.getParentId())) {
                roots.add(menu);
                continue;
            }
            responseMap.get(menu.getParentId()).getChildren().add(menu);
        }
        return roots;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> roleMenuIds(Long roleId) {
        SysRole role = getRole(roleId);
        return role.getMenus().stream()
                .map(SysMenu::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional
    public Set<Long> saveRoleMenus(Long roleId, Set<Long> menuIds) {
        SysRole role = getRole(roleId);
        List<SysMenu> menus = menuIds == null || menuIds.isEmpty()
                ? List.of()
                : menuRepository.findByIdIn(menuIds);

        role.setMenus(new LinkedHashSet<>(menus));
        roleRepository.save(role);
        return roleMenuIds(roleId);
    }

    private SysRole getRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在：" + roleId));
    }

    private SysMenu getMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("菜单不存在：" + menuId));
    }

    private void validateRequest(MenuRequest request) {
        if (!StringUtils.hasText(request.getMenuCode())) {
            throw new IllegalArgumentException("菜单编码不能为空");
        }
        if (!StringUtils.hasText(request.getMenuName())) {
            throw new IllegalArgumentException("菜单名称不能为空");
        }
        if (!StringUtils.hasText(request.getMenuType())) {
            throw new IllegalArgumentException("菜单类型不能为空");
        }
    }

    private void fillMenu(SysMenu menu, MenuRequest request) {
        menu.setParentId(request.getParentId());
        menu.setMenuCode(request.getMenuCode());
        menu.setMenuName(request.getMenuName());
        menu.setRouteKey(request.getRouteKey());
        menu.setMenuType(request.getMenuType());
        menu.setPermissionCode(request.getPermissionCode());
        menu.setSortOrder(request.getSortOrder() == null ? 999 : request.getSortOrder());
        menu.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : ENABLED);
    }

    private MenuResponse toResponse(SysMenu menu) {
        MenuResponse response = new MenuResponse();
        response.setId(menu.getId());
        response.setParentId(menu.getParentId());
        response.setMenuCode(menu.getMenuCode());
        response.setMenuName(menu.getMenuName());
        response.setRouteKey(menu.getRouteKey());
        response.setMenuType(menu.getMenuType());
        response.setPermissionCode(menu.getPermissionCode());
        response.setSortOrder(menu.getSortOrder());
        response.setStatus(menu.getStatus());
        return response;
    }
}
