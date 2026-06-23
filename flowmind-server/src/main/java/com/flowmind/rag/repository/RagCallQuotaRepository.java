package com.flowmind.rag.repository;

import com.flowmind.rag.entity.RagCallQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface RagCallQuotaRepository extends JpaRepository<RagCallQuota, Long> {

    /**
     * 以悲观写锁查询某日配额记录，保证并发计数安全。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from RagCallQuota q where q.quotaDate = :quotaDate")
    Optional<RagCallQuota> findForUpdate(@Param("quotaDate") LocalDate quotaDate);
}
