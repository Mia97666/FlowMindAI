package com.flowmind.common;

import com.flowmind.user.entity.SysRole;
import com.flowmind.user.entity.SysMenu;
import com.flowmind.user.entity.SysUser;
import com.flowmind.user.repository.SysMenuRepository;
import com.flowmind.user.repository.SysRoleRepository;
import com.flowmind.user.repository.SysUserRepository;
import com.flowmind.form.entity.FieldDefinition;
import com.flowmind.form.entity.FormDefinition;
import com.flowmind.form.repository.FieldDefinitionRepository;
import com.flowmind.form.repository.FormDefinitionRepository;
import com.flowmind.form.service.FormDefinitionService;
import com.flowmind.workflow.entity.WorkflowDefinition;
import com.flowmind.workflow.repository.WorkflowDefinitionRepository;
import com.flowmind.workflow.service.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 本地演示数据初始化器。
 *
 * 该初始化器只在缺少对应角色/用户时插入数据，
 * 不会覆盖管理员在系统里后续维护的数据。
 */
@Component
@RequiredArgsConstructor
public class FlowMindDataInitializer implements CommandLineRunner {

    private static final String ENABLED = "ENABLED";

    private final SysRoleRepository roleRepository;

    private final SysMenuRepository menuRepository;

    private final SysUserRepository userRepository;

    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    private final FieldDefinitionRepository fieldDefinitionRepository;

    private final FormDefinitionRepository formDefinitionRepository;

    private final FormDefinitionService formDefinitionService;

    private final WorkflowDefinitionService workflowDefinitionService;

    @Override
    public void run(String... args) {
        Map<String, String> roles = Map.of(
                "ADMIN", "系统管理员",
                "WORKFLOW_ADMIN", "流程管理员",
                "FINANCE", "财务人员",
                "HR", "人力资源",
                "MANAGER", "部门负责人",
                "EMPLOYEE", "普通员工"
        );

        roles.forEach((code, name) -> ensureRole(code, name));
        ensureMenus();
        ensureRoleMenus();

        SysUser admin = ensureUser("admin", "系统管理员", "管理部", "平台管理员", "NORMAL", List.of("ADMIN"));
        SysUser manager = ensureUser("lisi", "李四", "研发部", "研发负责人", "NORMAL", List.of("MANAGER"));
        ensureUser("finance", "王五", "财务部", "财务经理", "NORMAL", List.of("FINANCE"));
        ensureUser("zhangsan", "张三", "研发部", "研发工程师", "NORMAL", List.of("EMPLOYEE"));
        ensureUser("ai_approver", "AI审批员", "数字员工", "智能审批 Agent", "AI", List.of("WORKFLOW_ADMIN"));

        userRepository.findByUsername("zhangsan").ifPresent(user -> {
            if (user.getManagerId() == null) {
                user.setManagerId(manager.getId());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        });

        userRepository.findByUsername("admin").ifPresent(user -> {
            if (user.getManagerId() == null) {
                user.setManagerId(admin.getId());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        });

        ensureFieldDefinitions();
        ensureFormDefinitions();
        ensurePurchaseWorkflow();
    }

    private SysRole ensureRole(String code, String name) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> {
                    SysRole role = new SysRole();
                    role.setCode(code);
                    role.setName(name);
                    role.setDescription("系统初始化角色：" + name);
                    role.setCreatedAt(LocalDateTime.now());
                    return roleRepository.save(role);
                });
    }

    private SysUser ensureUser(
            String username,
            String realName,
            String department,
            String position,
            String userType,
            List<String> roleCodes
    ) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    SysUser user = new SysUser();
                    user.setUsername(username);
                    user.setRealName(realName);
                    user.setDepartment(department);
                    user.setPosition(position);
                    user.setEmail(username + "@flowmind.local");
                    user.setStatus(ENABLED);
                    user.setUserType(userType);
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    user.setRoles(new LinkedHashSet<>(roleCodes.stream()
                            .map(code -> roleRepository.findByCode(code)
                                    .orElseThrow(() -> new IllegalStateException("缺少初始化角色：" + code)))
                            .toList()));
                    return userRepository.save(user);
                });
    }

    /**
     * 初始化产品确认后的菜单层级。
     *
     * 顶部菜单用于一级模块切换；左侧菜单挂在对应顶部菜单下，
     * 其中流程设计模块包含流程管理和表单管理两个分组。
     */
    private void ensureMenus() {
        SysMenu dashboard = ensureMenu(null, "dashboard", "运行总览", "TOP", "dashboard", "dashboard:view", 10);
        SysMenu design = ensureMenu(null, "design", "流程设计", "TOP", "design", "design:view", 20);
        SysMenu approval = ensureMenu(null, "approval", "审批", "TOP", "approval", "approval:view", 30);
        SysMenu knowledge = ensureMenu(null, "knowledge", "知识库", "TOP", "knowledge", "knowledge:view", 40);
        SysMenu user = ensureMenu(null, "user", "用户管理", "TOP", "user", "user:view", 50);

        ensureMenu(dashboard, "dashboard.overview", "运行总览", "PAGE", "overview", "dashboard:overview", 11);
        ensureMenu(dashboard, "dashboard.instances", "实例追踪", "PAGE", "instanceTrace", "dashboard:instances", 12);

        SysMenu workflowGroup = ensureMenu(design, "design.workflow", "流程管理", "GROUP", "workflowList", "workflow:list", 21);
        ensureMenu(workflowGroup, "design.workflow.designer", "工作流设计", "PAGE", "workflowDesigner", "workflow:design", 22);
        SysMenu formGroup = ensureMenu(design, "design.form", "表单管理", "GROUP", "formList", "form:list", 23);
        ensureMenu(formGroup, "design.form.field", "表单字段管理", "PAGE", "fieldList", "field:list", 24);
        ensureMenu(formGroup, "design.form.designer", "表单设计", "PAGE", "formDesigner", "form:design", 25);

        ensureMenu(approval, "approval.todo", "待审批", "PAGE", "todo", "approval:todo", 31);
        ensureMenu(approval, "approval.done", "已审批", "PAGE", "done", "approval:done", 32);
        ensureMenu(approval, "approval.start", "发起审批", "PAGE", "startApproval", "approval:start", 33);

        ensureMenu(knowledge, "knowledge.documents", "制度文档", "PAGE", "documents", "knowledge:documents", 41);
        ensureMenu(knowledge, "knowledge.chunks", "Chunk 查看", "PAGE", "chunks", "knowledge:chunks", 42);
        ensureMenu(knowledge, "knowledge.rag", "RAG 测试", "PAGE", "ragTest", "knowledge:rag", 43);
        ensureMenu(knowledge, "knowledge.config", "知识库配置", "PAGE", "kbConfig", "knowledge:config", 44);

        ensureMenu(user, "user.profile", "个人信息", "PAGE", "profile", "user:profile", 51);
        ensureMenu(user, "user.role", "角色管理", "PAGE", "roleList", "role:list", 52);
        ensureMenu(user, "user.list", "用户管理", "PAGE", "userList", "user:list", 53);
        ensureMenu(user, "user.menu", "菜单管理", "PAGE", "menuPermission", "menu:permission", 54);
    }

    private SysMenu ensureMenu(
            SysMenu parent,
            String menuCode,
            String menuName,
            String menuType,
            String routeKey,
            String permissionCode,
            Integer sortOrder
    ) {
        SysMenu menu = menuRepository.findByMenuCode(menuCode)
                .orElseGet(SysMenu::new);
        menu.setParentId(parent == null ? null : parent.getId());
        menu.setMenuCode(menuCode);
        menu.setMenuName(menuName);
        menu.setMenuType(menuType);
        menu.setRouteKey(routeKey);
        menu.setPermissionCode(permissionCode);
        menu.setSortOrder(sortOrder);
        menu.setStatus(ENABLED);
        if (menu.getCreatedAt() == null) {
            menu.setCreatedAt(LocalDateTime.now());
        }
        menu.setUpdatedAt(LocalDateTime.now());
        return menuRepository.save(menu);
    }

    /**
     * 给初始化角色写入菜单权限。
     *
     * 管理员默认拥有全部菜单；其他角色只拥有其日常工作所需入口。
     */
    private void ensureRoleMenus() {
        grantMenus("ADMIN", menuRepository.findAll().stream()
                .map(SysMenu::getMenuCode)
                .toList());
        grantMenus("WORKFLOW_ADMIN", List.of(
                "dashboard", "dashboard.overview", "dashboard.instances",
                "design", "design.workflow", "design.workflow.designer",
                "design.form", "design.form.field", "design.form.designer",
                "knowledge", "knowledge.documents", "knowledge.chunks", "knowledge.rag", "knowledge.config"
        ));
        grantMenus("FINANCE", List.of(
                "dashboard", "dashboard.overview", "dashboard.instances",
                "approval", "approval.todo", "approval.done", "approval.start",
                "knowledge", "knowledge.documents", "knowledge.chunks", "knowledge.rag"
        ));
        grantMenus("MANAGER", List.of(
                "dashboard", "dashboard.overview", "dashboard.instances",
                "approval", "approval.todo", "approval.done", "approval.start"
        ));
        grantMenus("EMPLOYEE", List.of(
                "dashboard", "dashboard.overview", "dashboard.instances",
                "approval", "approval.todo", "approval.done", "approval.start",
                "user", "user.profile"
        ));
    }

    private void grantMenus(String roleCode, List<String> menuCodes) {
        SysRole role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("缺少初始化角色：" + roleCode));
        Set<SysMenu> menus = new LinkedHashSet<>();
        for (String menuCode : menuCodes) {
            SysMenu menu = menuRepository.findByMenuCode(menuCode)
                    .orElseThrow(() -> new IllegalStateException("缺少初始化菜单：" + menuCode));
            menus.add(menu);
        }
        role.setMenus(menus);
        roleRepository.save(role);
    }

    /**
     * 初始化一个可演示的采购审批流程。
     *
     * 流程路径：
     * START -> AI风险检测 -> 条件路由
     * 低风险：AI 自动审批通过 -> 通知 -> END
     * 中风险：直属领导审批 -> 通知 -> END
     * 高风险：财务审批 -> 通知 -> END
     */
    private void ensurePurchaseWorkflow() {
        Optional<WorkflowDefinition> existingWorkflow =
                workflowDefinitionRepository.findByCode("PURCHASE_APPROVAL");
        if (existingWorkflow.isPresent()) {
            WorkflowDefinition definition = existingWorkflow.get();
            boolean upgraded = upgradePurchaseWorkflowIfNeeded(definition);
            if (definition.getPublishedVersionId() == null || upgraded) {
                workflowDefinitionService.publish(definition.getId());
            }
            return;
        }

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setCode("PURCHASE_APPROVAL");
        definition.setName("采购审批流程");
        definition.setDescription(purchaseWorkflowDescription());
        definition.setFormJson(purchaseInlineFormJson());
        definition.setDefinitionJson(purchaseWorkflowDefinitionJson());
        definition.setBpmnXml("<bpmn:process id=\"PURCHASE_APPROVAL\" name=\"采购审批流程\" />");
        definition.setVersion(1);
        definition.setStatus("PUBLISHED");
        definition.setEnabled(true);
        definition.setPublishedAt(LocalDateTime.now());
        definition.setCreatedAt(LocalDateTime.now());
        definition.setUpdatedAt(LocalDateTime.now());
        WorkflowDefinition savedDefinition = workflowDefinitionRepository.save(definition);
        workflowDefinitionService.publish(savedDefinition.getId());
    }

    /**
     * 已经初始化过的本地演示库，可能还是旧的“低/中风险都走人工”模板。
     * 这里只升级系统示例流程，不处理管理员后续新建的其他流程。
     */
    private boolean upgradePurchaseWorkflowIfNeeded(WorkflowDefinition definition) {
        String currentJson = definition.getDefinitionJson() == null
                ? ""
                : definition.getDefinitionJson();
        if (currentJson.contains("e_low_auto")
                && currentJson.contains("autoApproveMaxScore")) {
            return false;
        }

        definition.setDescription(purchaseWorkflowDescription());
        definition.setFormJson(purchaseInlineFormJson());
        definition.setDefinitionJson(purchaseWorkflowDefinitionJson());
        definition.setBpmnXml("<bpmn:process id=\"PURCHASE_APPROVAL\" name=\"采购审批流程\" />");
        definition.setStatus("PUBLISHED");
        definition.setEnabled(true);
        definition.setUpdatedAt(LocalDateTime.now());
        workflowDefinitionRepository.save(definition);
        return true;
    }

    private String purchaseWorkflowDescription() {
        return "系统初始化示例流程：支持 AI/RAG 风险评分、低风险自动审批、高风险人工审批和全程审计。";
    }

    private String purchaseInlineFormJson() {
        return """
                {"fields":[
                  {"key":"item","label":"采购物品","type":"TEXT","required":true,"placeholder":"例如 MacBook Pro"},
                  {"key":"quantity","label":"采购数量","type":"NUMBER","required":true,"placeholder":"例如 30"},
                  {"key":"amount","label":"采购金额","type":"AMOUNT","required":true,"placeholder":"例如 450000"},
                  {"key":"purpose","label":"采购用途","type":"TEXTAREA","required":true,"placeholder":"说明采购背景和业务必要性"},
                  {"key":"expectedDate","label":"期望到货时间","type":"DATE","required":false}
                ]}
                """;
    }

    private String purchaseWorkflowDefinitionJson() {
        return """
                {
                  "nodes": [
                    {"id":"start","nodeType":"START","name":"开始","config":{"formCode":"PURCHASE_APPLY_FORM"}},
                    {"id":"ai_risk","nodeType":"AI_RISK_CHECK","name":"AI 风险检测","config":{"formCode":"PURCHASE_APPLY_FORM","aiStrategy":"采购制度风险评分策略","threshold":70,"autoApproveMaxScore":30,"autoApproveActor":"ai_approver","highRiskReceivers":"finance,admin"}},
                    {"id":"risk_gateway","nodeType":"CONDITION","name":"风险等级路由","config":{"formCode":"PURCHASE_APPLY_FORM","defaultRoute":"manager_approve"}},
                    {"id":"manager_approve","nodeType":"APPROVAL","name":"中风险直属领导审批","config":{"formCode":"PURCHASE_APPLY_FORM","assigneeType":"MANAGER"}},
                    {"id":"finance_approve","nodeType":"APPROVAL","name":"高风险财务审批","config":{"formCode":"PURCHASE_APPLY_FORM","assigneeType":"ROLE","assigneeValue":"FINANCE"}},
                    {"id":"notify_done","nodeType":"NOTIFY","name":"结果通知","config":{"receivers":"starter","title":"采购审批流转提醒","content":"流程 {title} 已完成当前审批路径，风险等级：{riskLevel}，评分：{riskScore}。"}},
                    {"id":"end","nodeType":"END","name":"结束","config":{}}
                  ],
                  "edges": [
                    {"id":"e_start_ai","source":"start","target":"ai_risk"},
                    {"id":"e_ai_gateway","source":"ai_risk","target":"risk_gateway"},
                    {"id":"e_high","source":"risk_gateway","target":"finance_approve","condition":"riskLevel == \"HIGH\"","label":"高风险人工审批"},
                    {"id":"e_low_auto","source":"risk_gateway","target":"notify_done","condition":"riskLevel == \"LOW\"","label":"低风险AI自动通过"},
                    {"id":"e_medium","source":"risk_gateway","target":"manager_approve","condition":"default","label":"中风险人工审批"},
                    {"id":"e_finance_notify","source":"finance_approve","target":"notify_done"},
                    {"id":"e_manager_notify","source":"manager_approve","target":"notify_done"},
                    {"id":"e_notify_end","source":"notify_done","target":"end"}
                  ]
                }
                """;
    }

    /**
     * 初始化表单字段定义。
     *
     * 字段定义会被后续表单设计器复用，示例字段和采购审批流程的 formJson 保持一致。
     */
    private void ensureFieldDefinitions() {
        ensureField("item", "采购物品", "TEXT", "采购申请中的物品或服务名称", true);
        ensureField("quantity", "采购数量", "NUMBER", "采购数量，供 AI 风险评分参考", true);
        ensureField("amount", "采购金额", "AMOUNT", "采购总金额，条件路由和风险评分重点字段", true);
        ensureField("purpose", "采购用途", "TEXTAREA", "采购背景、业务必要性和预算说明", true);
        ensureField("expectedDate", "期望到货时间", "DATE", "期望交付或到货日期", false);
        ensureField("invoiceNo", "发票号码", "TEXT", "报销或采购场景可复用字段", false);
    }

    private void ensureField(
            String fieldKey,
            String fieldName,
            String fieldType,
            String description,
            boolean required
    ) {
        if (fieldDefinitionRepository.findByFieldKey(fieldKey).isPresent()) {
            return;
        }
        FieldDefinition definition = new FieldDefinition();
        definition.setFieldKey(fieldKey);
        definition.setFieldName(fieldName);
        definition.setFieldType(fieldType);
        definition.setSourceType("SYSTEM");
        definition.setValidationJson("{\"required\":" + required + "}");
        definition.setDescription(description);
        definition.setStatus(ENABLED);
        definition.setCreatedAt(LocalDateTime.now());
        definition.setUpdatedAt(LocalDateTime.now());
        fieldDefinitionRepository.save(definition);
    }

    /**
     * 初始化一份采购申请表单模板。
     *
     * 后续工作流节点只需要绑定 formCode，就能复用同一份表单渲染规则。
     */
    private void ensureFormDefinitions() {
        Optional<FormDefinition> existingForm =
                formDefinitionRepository.findByFormCode("PURCHASE_APPLY_FORM");
        if (existingForm.isPresent()) {
            FormDefinition definition = existingForm.get();
            if (definition.getPublishedVersionId() == null) {
                formDefinitionService.publish(definition.getId());
            }
            return;
        }
        FormDefinition definition = new FormDefinition();
        definition.setFormCode("PURCHASE_APPLY_FORM");
        definition.setFormName("采购申请表单");
        definition.setCategory("采购");
        definition.setVersion(1);
        definition.setStatus("PUBLISHED");
        definition.setEnabled(true);
        definition.setSchemaJson("""
                {"fields":[
                  {"id":"item","fieldKey":"item","label":"采购物品","componentType":"TEXT","required":true,"span":12,"placeholder":"例如 MacBook Pro"},
                  {"id":"quantity","fieldKey":"quantity","label":"采购数量","componentType":"NUMBER","required":true,"span":12,"placeholder":"例如 30"},
                  {"id":"amount","fieldKey":"amount","label":"采购金额","componentType":"AMOUNT","required":true,"span":12,"placeholder":"例如 450000"},
                  {"id":"expectedDate","fieldKey":"expectedDate","label":"期望到货时间","componentType":"DATE","required":false,"span":12,"placeholder":"请选择日期"},
                  {"id":"purpose","fieldKey":"purpose","label":"采购用途","componentType":"TEXTAREA","required":true,"span":24,"placeholder":"说明采购背景和业务必要性"}
                ]}
                """);
        definition.setDescription("系统初始化采购申请表单，供采购审批流程发起、审批、查看复用。");
        definition.setPublishedAt(LocalDateTime.now());
        definition.setCreatedAt(LocalDateTime.now());
        definition.setUpdatedAt(LocalDateTime.now());
        FormDefinition savedDefinition = formDefinitionRepository.save(definition);
        formDefinitionService.publish(savedDefinition.getId());
    }
}
