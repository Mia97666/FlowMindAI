package com.flowmind.rag.repository;

import com.flowmind.rag.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long>, JpaSpecificationExecutor<KnowledgeChunk> {

    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    Optional<KnowledgeChunk> findByVectorId(String vectorId);

    /**
     * 简单关键词检索。
     *
     * 当前使用 PostgreSQL LIKE。
     * 后续可以升级为 PostgreSQL Full Text Search 或 Elasticsearch。
     */
    @Query("""
            select c
            from KnowledgeChunk c
            where c.content like concat('%', :keyword, '%')
            order by c.id desc
            """)
    List<KnowledgeChunk> searchByKeyword(String keyword);
}