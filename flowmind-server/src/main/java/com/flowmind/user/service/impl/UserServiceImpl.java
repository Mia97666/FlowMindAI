package com.flowmind.user.service.impl;

import com.flowmind.common.exception.ConflictException;
import com.flowmind.common.exception.NotFoundException;
import com.flowmind.common.util.SpecificationBuilder;
import com.flowmind.user.dto.UserRequest;
import com.flowmind.user.entity.SysRole;
import com.flowmind.user.entity.SysUser;
import com.flowmind.user.repository.SysRoleRepository;
import com.flowmind.user.repository.SysUserRepository;
import com.flowmind.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_STATUS = "ENABLED";

    private static final String DEFAULT_USER_TYPE = "NORMAL";

    private final SysUserRepository userRepository;

    private final SysRoleRepository roleRepository;

    @Override
    public SysUser create(UserRequest request) {
        userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
            throw new ConflictException("用户名已存在：" + user.getUsername());
        });

        SysUser user = new SysUser();
        fillUser(user, request);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public SysUser update(Long id, UserRequest request) {
        SysUser user = get(id);
        userRepository.findByUsername(request.getUsername())
                .filter(existed -> !existed.getId().equals(id))
                .ifPresent(existed -> {
                    throw new IllegalArgumentException("用户名已存在：" + existed.getUsername());
                });
        fillUser(user, request);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public List<SysUser> list() {
        return userRepository.findAll();
    }

    @Override
    public Page<SysUser> page(
            String username,
            String realName,
            String department,
            String status,
            Pageable pageable
    ) {
        Specification<SysUser> specification = SpecificationBuilder.<SysUser>builder()
                .like("username", username)
                .like("realName", realName)
                .equal("department", department)
                .equal("status", status)
                .build();
        return userRepository.findAll(specification, pageable);
    }

    @Override
    public SysUser get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("用户", id));
    }

    @Override
    public SysUser bindRole(Long userId, Long roleId) {
        SysUser user = get(userId);
        SysRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在：" + roleId));

        user.getRoles().add(role);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public SysUser updateRoles(Long userId, List<String> roleCodes) {
        SysUser user = get(userId);
        Set<SysRole> roles = new LinkedHashSet<>();
        for (String roleCode : roleCodes == null ? List.<String>of() : roleCodes) {
            SysRole role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new IllegalArgumentException("角色不存在：" + roleCode));
            roles.add(role);
        }
        user.setRoles(roles);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 将请求对象中的可编辑字段复制到实体。
     *
     * 单独抽出该方法，是为了让创建和更新保持一致的字段处理逻辑。
     */
    private void fillUser(SysUser user, UserRequest request) {
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setPosition(request.getPosition());
        user.setManagerId(request.getManagerId());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        } else if (user.getStatus() == null) {
            user.setStatus(DEFAULT_STATUS);
        }
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        } else if (user.getUserType() == null) {
            user.setUserType(DEFAULT_USER_TYPE);
        }

        if (request.getRoleCodes() != null) {
            Set<SysRole> roles = new LinkedHashSet<>();
            for (String roleCode : request.getRoleCodes()) {
                SysRole role = roleRepository.findByCode(roleCode)
                        .orElseThrow(() -> new IllegalArgumentException("角色不存在：" + roleCode));
                roles.add(role);
            }
            user.setRoles(roles);
        }
    }
}
