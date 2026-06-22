package com.flowmind.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流实例。
 *
 * 含义：
 * 某个用户基于某个流程定义，发起的一次真实审批流程。
 *
 * 举例：
 * workflow_definition 表里有一个"采购审批流程"模板。
 * 用户提交"采购 30 台 MacBook，总金额 45 万"的申请后，
 * 系统会创建一条 workflow_instance 记录。
 *
 * definition 是模板；
 * instance 是模板运行后产生的实例。
 */
@Data
@Entity
@Table(name = "workflow_instance")
public class WorkflowInstance {

    /**
     * 主键ID。
     *
     * 每发起一次审批，都会生成一个新的实例ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 流程定义ID。
     *
     * 对应 workflow_definition.id。
     * 表示该实例是基于哪个流程模板启动的。
     */
    private Long definitionId;

    /**
     * 流程定义编码。
     *
     * 冗余保存一份，方便后续查询和日志分析。
     * 示例：PURCHASE_APPROVAL
     */
    private String definitionCode;

    /**
     * 流程定义名称。
     *
     * 冗余保存一份，避免流程定义名称后续变更影响历史记录。
     * 示例：采购审批流程
     */
    private String definitionName;

    /**
     * 流程定义版本。
     *
     * 实例启动后固定绑定一个版本，
     * 后续管理员重新发布新版本不会影响该实例。
     */
    private Integer definitionVersion;

    /**
     * 工作流发布版本 ID。
     *
     * 指向 fm_workflow_version.id，实例启动后固定不变。
     */
    private Long definitionVersionId;

    /**
     * 发起节点绑定的表单版本 ID。
     *
     * 用于回放发起表单，避免表单模板变更影响历史实例。
     */
    private Long startFormVersionId;

    /**
     * 发起人。
     *
     * 当前阶段先用字符串模拟。
     * 后续接入登录系统后，可以改为 userId。
     */
    private String starter;

    /**
     * 业务标题。
     *
     * 示例：
     * 采购30台MacBook审批
     * 6月份差旅报销审批
     */
    private String title;

    /**
     * 用户提交的业务数据。
     *
     * 使用 JSON 字符串保存。
     *
     * 示例：
     * {
     *   "amount": 450000,
     *   "item": "MacBook",
     *   "quantity": 30,
     *   "purpose": "研发团队扩容"
     * }
     */
    @Column(columnDefinition = "TEXT")
    private String businessDataJson;

    /**
     * 当前节点ID。
     *
     * 表示流程目前走到哪个节点。
     * 示例：ai_risk_check、manager_approve、finance_approve
     */
    private String currentNodeId;

    /**
     * 当前节点名称。
     *
     * 冗余保存给列表页展示，避免前端每次都重新解析 definitionJson。
     */
    private String currentNodeName;

    /**
     * 流程状态。
     *
     * RUNNING：运行中
     * COMPLETED：已完成
     * REJECTED：已拒绝
     * CANCELED：已取消
     */
    private String status;

    /**
     * AI风险分数。
     *
     * 后续 AI_RISK_CHECK 节点执行后写入。
     * 0-100，分数越高风险越高。
     */
    private Integer riskScore;

    /**
     * AI风险等级。
     */
    private String riskLevel;

    /**
     * AI风险原因。
     *
     * 后续 AI_RISK_CHECK 节点执行后写入。
     */
    @Column(columnDefinition = "TEXT")
    private String riskReason;

    /**
     * AI 给出的处理建议。
     */
    @Column(columnDefinition = "TEXT")
    private String aiSuggestion;

    /**
     * AI/RAG 来源引用 JSON 快照。
     */
    @Column(columnDefinition = "TEXT")
    private String ragSourcesJson;

    /**
     * 乐观锁版本号。
     *
     * 防止两个审批人同时对同一流程实例的待办任务操作时，
     * 流程状态被覆盖。
     */
    @Version
    private Long version;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 流程完成时间。
     */
    private LocalDateTime completedAt;
}
