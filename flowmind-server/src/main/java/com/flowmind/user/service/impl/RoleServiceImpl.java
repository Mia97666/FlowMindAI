package com.flowmind.user.service.impl;

import com.flowmind.common.exception.ConflictException;
import com.flowmind.common.exception.NotFoundException;
import com.flowmind.common.util.SpecificationBuilder;
import com.flowmind.user.dto.RoleRequest;
import com.flowmind.user.entity.SysRole;
import com.flowmind.user.repository.SysRoleRepository;
import com.flowmind.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色服务实现。
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleRepository roleRepository;

    @Override
    public SysRole create(RoleRequest request) {
        roleRepository.findByCode(request.getCode()).ifPresent(role -> {
            throw new ConflictException("角色编码已存在：" + role.getCode());
        });

        SysRole role = new SysRole();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setCreatedAt(LocalDateTime.now());
        return roleRepository.save(role);
    }

    @Override
    public List<SysRole> list() {
        return roleRepository.findAll();
    }

    @Override
    public Page<SysRole> page(
            String code,
            String name,
            Pageable pageable
    ) {
        Specification<SysRole> specification = SpecificationBuilder.<SysRole>builder()
                .like("code", code)
                .like("name", name)
                .build();
        return roleRepository.findAll(specification, pageable);
    }

    @Override
    public SysRole get(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("角色", id));
    }

    @Override
    public SysRole update(Long id, RoleRequest request) {
        SysRole role = get(id);
        roleRepository.findByCode(request.getCode())
                .filter(existed -> !existed.getId().equals(id))
                .ifPresent(existed -> {
                    throw new IllegalArgumentException("角色编码已存在：" + existed.getCode());
                });
        if (request.getCode() != null) {
            role.setCode(request.getCode());
        }
        if (request.getName() != null) {
            role.setName(request.getName());
        }
        role.setDescription(request.getDescription());
        return roleRepository.save(role);
    }
}
