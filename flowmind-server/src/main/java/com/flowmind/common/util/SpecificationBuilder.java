package com.flowmind.common.util;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * JPA Specification 通用构建器。
 *
 * 链式 API 构建动态查询条件，消除各 Service 中重复的 Specification 代码。
 *
 * 使用示例：
 * <pre>{@code
 * Specification<WorkflowInstance> spec = SpecificationBuilder.<WorkflowInstance>builder()
 *     .equal("starter", starter)
 *     .like("title", title)
 *     .equal("status", status)
 *     .build();
 * }</pre>
 */
public class SpecificationBuilder<T> {

    private final List<Function<SpecContext<T>, Predicate>> predicates = new ArrayList<>();

    private SpecificationBuilder() {
    }

    public static <T> SpecificationBuilder<T> builder() {
        return new SpecificationBuilder<>();
    }

    public SpecificationBuilder<T> equal(String field, String value) {
        if (StringUtils.hasText(value)) {
            predicates.add(ctx -> ctx.cb.equal(ctx.root.get(field), value));
        }
        return this;
    }

    public SpecificationBuilder<T> equal(String field, Long value) {
        if (value != null) {
            predicates.add(ctx -> ctx.cb.equal(ctx.root.get(field), value));
        }
        return this;
    }

    public SpecificationBuilder<T> like(String field, String value) {
        if (StringUtils.hasText(value)) {
            predicates.add(ctx -> ctx.cb.like(
                    ctx.cb.lower(ctx.root.get(field)),
                    "%" + value.toLowerCase(Locale.ROOT) + "%"));
        }
        return this;
    }

    public SpecificationBuilder<T> in(String field, List<String> values) {
        if (values != null && !values.isEmpty()) {
            predicates.add(ctx -> ctx.root.get(field).in(values));
        }
        return this;
    }

    public Specification<T> build() {
        return (root, query, cb) -> {
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            SpecContext<T> ctx = new SpecContext<>(root, cb);
            Predicate[] arr = predicates.stream()
                    .map(fn -> fn.apply(ctx))
                    .toArray(Predicate[]::new);
            return cb.and(arr);
        };
    }

    private record SpecContext<T>(jakarta.persistence.criteria.Root<T> root,
                                  jakarta.persistence.criteria.CriteriaBuilder cb) {
    }
}