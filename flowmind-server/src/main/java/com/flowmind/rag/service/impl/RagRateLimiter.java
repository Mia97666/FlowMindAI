package com.flowmind.rag.service.impl;

import com.flowmind.common.exception.BusinessException;
import com.flowmind.common.exception.ErrorCode;
import com.flowmind.rag.entity.RagCallQuota;
import com.flowmind.rag.repository.RagCallQuotaRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * RAG 调用每日限流器。
 *
 * 测试阶段防止 RAG 接口被滥用：每自然日全局最多调用 {@code daily-limit} 次，
 * 达到上限后抛出 {@link ErrorCode#RAG_RATE_LIMIT_EXCEEDED}。
 *
 * 计数持久化到数据库（{@link RagCallQuota}），服务重启不会清零。
 * 通过 {@code flowmind.rag.rate-limit.enabled} 可整体关闭限流。
 */
@Service
public class RagRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RagRateLimiter.class);

    private final RagCallQuotaRepository quotaRepository;

    @Value("${flowmind.rag.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${flowmind.rag.rate-limit.daily-limit:10}")
    private int dailyLimit;

    public RagRateLimiter(RagCallQuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    @PostConstruct
    void logStatus() {
        if (enabled) {
            log.info("RAG 限流已启用，每日上限 {} 次", dailyLimit);
        } else {
            log.warn("RAG 限流已关闭（flowmind.rag.rate-limit.enabled=false）");
        }
    }

    /**
     * 检查今日配额并累加一次调用计数，超限则抛出业务异常。
     *
     * 限流关闭时直接放行，不计数。
     */
    @Transactional
    public void checkAndIncrement() {
        if (!enabled) {
            return;
        }

        LocalDate today = LocalDate.now();
        RagCallQuota quota = loadOrCreate(today);

        if (quota.getCallCount() >= dailyLimit) {
            log.warn("RAG 调用被限流：今日已用 {} / {}", quota.getCallCount(), dailyLimit);
            throw new BusinessException(
                    ErrorCode.RAG_RATE_LIMIT_EXCEEDED,
                    "防止token滥用，今日 RAG 调用次数已达上限（" + dailyLimit + " 次），请明天再试"
            );
        }

        quota.setCallCount(quota.getCallCount() + 1);
        quotaRepository.save(quota);
    }

    /**
     * 加载当日配额行，不存在则插入。
     *
     * 并发首次写入时可能违反 quotaDate 唯一约束，捕获后重新加载已落库的行。
     */
    private RagCallQuota loadOrCreate(LocalDate today) {
        return quotaRepository.findForUpdate(today).orElseGet(() -> {
            RagCallQuota created = new RagCallQuota();
            created.setQuotaDate(today);
            created.setCallCount(0);
            try {
                return quotaRepository.saveAndFlush(created);
            } catch (DataIntegrityViolationException e) {
                log.debug("并发插入当日配额行冲突，重新加载: {}", today);
                return quotaRepository.findForUpdate(today)
                        .orElseThrow(() -> e);
            }
        });
    }
}
