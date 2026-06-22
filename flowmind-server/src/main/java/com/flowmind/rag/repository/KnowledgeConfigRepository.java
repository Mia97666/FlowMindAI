package com.flowmind.rag.repository;

import com.flowmind.rag.entity.KnowledgeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeConfigRepository extends JpaRepository<KnowledgeConfig, Long> {
}