package com.flowmind.rag.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * RAG 调用每日配额计数。
 *
 * 测试阶段用于防止 RAG 接口被滥用，每个自然日一行记录，
 * 累计当天调用次数，达到上限后拒绝后续调用。
 */
@Data
@Entity
@Table(
        name = "rag_call_quota",
        uniqueConstraints = @UniqueConstraint(name = "uk_rag_call_quota_date", columnNames = "quota_date")
)
public class RagCallQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配额所属自然日，格式 yyyy-MM-dd，全局唯一。 */
    @Column(name = "quota_date", nullable = false, unique = true)
    private LocalDate quotaDate;

    /** 当天已调用次数。 */
    @Column(name = "call_count", nullable = false)
    private Integer callCount;
}
