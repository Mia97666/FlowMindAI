package com.flowmind.workflow.engine.support;

import com.flowmind.user.entity.SysUser;
import com.flowmind.user.repository.SysUserRepository;
import com.flowmind.workflow.engine.model.WorkflowNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 审批人解析器。
 *
 * 支持 USER、ROLE、MANAGER、DEPARTMENT_MANAGER、FORM_FIELD 等常见分配方式。
 */
@Component
@RequiredArgsConstructor
public class AssigneeResolver {

    private static final String ENABLED = "ENABLED";

    private final SysUserRepository userRepository;

    /**
     * 根据节点配置解析实际审批人 username。
     */
    public String resolve(
            WorkflowNode node,
            String starter,
            Map<String, Object> businessData
    ) {
        Map<String, Object> config = node.getConfig();
        String assigneeType = text(config.getOrDefault("assigneeType", "USER"));
        String assigneeValue = text(config.get("assigneeValue"));

        return switch (assigneeType.toUpperCase()) {
            case "ROLE" -> resolveRole(assigneeValue);
            case "MANAGER" -> resolveManager(starter);
            case "DEPARTMENT_MANAGER" -> resolveDepartmentManager(starter, businessData);
            case "FORM_FIELD" -> resolveFormField(assigneeValue, businessData);
            case "AI" -> "ai_approver";
            default -> assigneeValue == null || assigneeValue.isBlank() ? "admin" : assigneeValue;
        };
    }

    private String resolveRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "admin";
        }
        return userRepository.findByRoles_CodeAndStatusOrderByIdAsc(roleCode, ENABLED)
                .stream()
                .findFirst()
                .map(SysUser::getUsername)
                .orElse(roleCode.toLowerCase());
    }

    private String resolveManager(String starter) {
        return userRepository.findByUsername(starter)
                .flatMap(user -> user.getManagerId() == null
                        ? userRepository.findByRoles_CodeAndStatusOrderByIdAsc("MANAGER", ENABLED).stream().findFirst()
                        : userRepository.findById(user.getManagerId()))
                .map(SysUser::getUsername)
                .orElse("manager");
    }

    private String resolveDepartmentManager(
            String starter,
            Map<String, Object> businessData
    ) {
        String department = text(businessData.get("department"));
        if (department == null || department.isBlank()) {
            department = userRepository.findByUsername(starter)
                    .map(SysUser::getDepartment)
                    .orElse(null);
        }

        if (department == null || department.isBlank()) {
            return resolveManager(starter);
        }

        return userRepository.findByDepartmentAndRoles_CodeAndStatusOrderByIdAsc(
                        department,
                        "MANAGER",
                        ENABLED
                )
                .stream()
                .findFirst()
                .map(SysUser::getUsername)
                .orElse(resolveManager(starter));
    }

    private String resolveFormField(
            String fieldName,
            Map<String, Object> businessData
    ) {
        Object value = businessData.get(fieldName);
        return value == null ? "admin" : String.valueOf(value);
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
