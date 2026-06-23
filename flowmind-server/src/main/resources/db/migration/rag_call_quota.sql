-- prod 环境 application-prod.yml 使用 spring.jpa.hibernate.ddl-auto=validate，
-- 不会自动建表，部署前需手动执行本脚本创建 RAG 调用配额表。

CREATE TABLE IF NOT EXISTS rag_call_quota (
    id          BIGSERIAL    PRIMARY KEY,
    quota_date  DATE         NOT NULL,
    call_count  INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT  uk_rag_call_quota_date UNIQUE (quota_date)
);

COMMENT ON TABLE  rag_call_quota IS 'RAG 调用每日配额计数（测试阶段限流）';
COMMENT ON COLUMN rag_call_quota.quota_date IS '配额所属自然日';
COMMENT ON COLUMN rag_call_quota.call_count IS '当天已调用次数';
