package com.flowmind.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体。
 *
 * MVP 阶段先用数据库记录模拟站内信，后续可以在服务层扩展邮件、企微等渠道。
 */
@Data
@Entity
@Table(name = "notification_message")
public class NotificationMessage {

    /**
     * 通知主键。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 接收人 username。
     */
    private String receiver;

    /**
     * 通知标题。
     */
    private String title;

    /**
     * 通知内容。
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 业务类型，例如 WORKFLOW_TASK、HIGH_RISK、WORKFLOW_DONE。
     */
    private String bizType;

    /**
     * 业务ID，例如任务ID或流程实例ID。
     */
    private Long bizId;

    /**
     * 是否已读。
     */
    private Boolean readFlag;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 阅读时间。
     */
    private LocalDateTime readAt;
}
