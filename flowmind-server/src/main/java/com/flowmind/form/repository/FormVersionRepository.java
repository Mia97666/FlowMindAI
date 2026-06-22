package com.flowmind.form.repository;

import com.flowmind.form.entity.FormVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 表单版本数据访问。
 */
public interface FormVersionRepository extends JpaRepository<FormVersion, Long> {

    /**
     * 查询某个表单定义下的版本列表，版本号倒序。
     */
    List<FormVersion> findByFormDefinitionIdOrderByVersionDesc(Long formDefinitionId);

    /**
     * 查询某个表单定义下的最新版本。
     */
    Optional<FormVersion> findTopByFormDefinitionIdOrderByVersionDesc(Long formDefinitionId);

    /**
     * 查询某个表单编码的最新发布版本。
     */
    Optional<FormVersion> findTopByFormCodeOrderByVersionDesc(String formCode);
}
