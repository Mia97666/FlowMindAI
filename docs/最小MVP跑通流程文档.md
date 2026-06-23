# FlowMind AI 最小MVP跑通流程文档

> **文档版本**: 1.0  
> **适用场景**: 本地开发环境最小MVP流程验证  
> **演示流程**: 信贷审批流程（含 AI 自动审批 + 人工审批）  
> **最后更新**: 2026-06-22

---

## 目录

1. [前置准备](#1-前置准备)
2. [启动基础设施](#2-启动基础设施)
3. [启动后端服务](#3-启动后端服务)
4. [启动前端服务](#4-启动前端服务)
5. [上传信贷制度文档到知识库](#5-上传信贷制度文档到知识库)
6. [创建信贷审批表单](#6-创建信贷审批表单)
7. [设计信贷审批工作流](#7-设计信贷审批工作流)
8. [发起低风险审批申请（AI 自动审批）](#8-发起低风险审批申请ai-自动审批)
9. [发起高风险审批申请（切换身份走人工审批流程）](#9-发起高风险审批申请切换身份走人工审批流程)
10. [数据库落库验证](#10-数据库落库验证)
11. [附录：模拟的信贷制度文档](#11-附录模拟的信贷制度文档)

---

## 1. 前置准备

### 1.1 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21+ | 后端 Spring Boot 3.5.5 需要 |
| Node.js | 18+ | 前端 Vue 3 + Vite |
| Docker | 20+ | PostgreSQL + Qdrant 容器 |
| Maven | 3.9+ | 或使用项目内置 mvnw |
| 通义千问 API Key | - | 千问 (DashScope) 平台获取，用于 AI 审批和 RAG |

### 1.2 项目结构

```
FlowMindAI/
├── docker-compose.yml          # PostgreSQL + Qdrant 基础设施
├── flowmind-server/            # Spring Boot 后端
│   ├── src/main/java/com/flowmind/
│   │   ├── workflow/           # 工作流引擎（定义/实例/任务/节点处理器）
│   │   ├── form/               # 表单系统（定义/字段/版本/渲染）
│   │   ├── rag/                # RAG 知识库（文档/分块/检索/重排序）
│   │   ├── user/               # 用户/角色/菜单权限
│   │   ├── audit/              # AI 审计日志
│   │   ├── notification/       # 站内通知
│   │   ├── ai/                 # AI 风险检查
│   │   └── common/             # 全局配置（含数据初始化器）
│   └── src/main/resources/
│       ├── application.yml     # 主配置
│       └── application-dev.yml # 开发环境配置
└── flowmind-web/               # Vue 3 前端
    └── src/
        ├── api/index.js        # API 封装
        └── pages/              # 页面组件
```

### 1.3 已经设置好环境变量

```bash
export DASHSCOPE_API_KEY="sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

---

## 2. 启动基础设施

### 2.1 启动 PostgreSQL 和 Qdrant

```bash
cd /Users/senga/IdeaProjects/ming/FlowMindAI
docker-compose up -d
```

验证容器运行状态：

```bash
docker ps | grep flowmind
```

预期输出：

```
flowmind-postgres   ...   Up XX seconds   0.0.0.0:5432->5432/tcp
flowmind-qdrant     ...   Up XX seconds   0.0.0.0:6333-6334->6333-6334/tcp
```

### 2.2 验证 PostgreSQL 连接

```bash
docker exec -it flowmind-postgres psql -U flowmind -d flowmind -c "\dt"
```

初始状态下表为空（JPA 会在后端首次启动时自动建表）。

---

## 3. 启动后端服务

### 3.1 编译并启动

```bash
cd /Users/senga/IdeaProjects/ming/FlowMindAI/flowmind-server
./mvnw spring-boot:run
```

### 3.2 验证后端启动

```bash
curl http://localhost:8080/api/health
```

预期返回：

```json
{"code": 200, "message": "OK", "data": "UP"}
```

### 3.3 数据初始化检查

后端启动后，`FlowMindDataInitializer` 会自动初始化以下数据：

**用户表 (sys_user)**: 5 个用户

| username | realName | department | position | 角色 | 说明 |
|----------|----------|------------|----------|------|------|
| admin | 系统管理员 | 管理部 | 平台管理员 | ADMIN | 超级管理员 |
| lisi | 李四 | 研发部 | 研发负责人 | MANAGER | 部门负责人 |
| finance | 王五 | 财务部 | 财务经理 | FINANCE | 财务审批人 |
| zhangsan | 张三 | 研发部 | 研发工程师 | EMPLOYEE | 普通员工（managerId=lisi） |
| ai_approver | AI审批员 | 数字员工 | 智能审批 Agent | WORKFLOW_ADMIN | AI 自动审批人 |

**角色表 (sys_role)**: 3 个角色 — ADMIN, MANAGER, FINANCE, EMPLOYEE, WORKFLOW_ADMIN, HR

**初始化流程**: 采购审批流程 (PURCHASE_APPROVAL) — 系统自带示例，包含 AI 风险检测 + 条件路由 + 人工审批

**初始化表单**: 采购申请表单 (PURCHASE_APPLY_FORM)

验证数据初始化：

```bash
# 查看用户
curl -s http://localhost:8080/api/users | python3 -m json.tool

# 查看流程定义
curl -s http://localhost:8080/api/workflows | python3 -m json.tool

# 查看表单
curl -s http://localhost:8080/api/forms | python3 -m json.tool
```

---

## 4. 启动前端服务

### 4.1 安装依赖并启动

```bash
cd /Users/senga/IdeaProjects/ming/FlowMindAI/flowmind-web
npm install
npm run dev
```

前端默认启动在 `http://localhost:5173`。

### 4.2 验证前端页面

在浏览器中打开 `http://localhost:5173`，确认能看到 FlowMind AI 主界面。

---

## 5. 上传信贷制度文档到知识库

### 5.1 创建信贷制度文档

在 `docs/` 目录下创建模拟的信贷制度文档（详细内容见 [附录](#11-附录模拟的信贷制度文档)）。

```bash
mkdir -p /Users/senga/IdeaProjects/ming/FlowMindAI/docs
```

创建以下两份文档：

**文档1**: `docs/信贷审批管理制度.md`
**文档2**: `docs/信贷风险评分标准.md`

### 5.2 通过 API 上传文档到知识库

```bash
# 上传信贷审批管理制度
curl -X POST http://localhost:8080/api/knowledge/documents/upload \
  -F "file=@docs/信贷审批管理制度.md" \
  -F "category=信贷制度"

# 上传信贷风险评分标准
curl -X POST http://localhost:8080/api/knowledge/documents/upload \
  -F "file=@docs/信贷风险评分标准.md" \
  -F "category=信贷制度"
```

### 5.3 验证文档上传和向量化

```bash
# 查看已上传文档
curl -s http://localhost:8080/api/knowledge/documents | python3 -m json.tool

# 在 Qdrant 中验证向量
curl -s http://localhost:6333/collections/flowmind_knowledge | python3 -m json.tool
```

### 5.4 在 UI 中验证 RAG 检索

打开前端页面 → 知识库 → RAG 测试，输入以下测试问题：

```
50万元以下的小额个人消费贷款，审批流程是怎样的？
```

AI 应能检索到制度文档并给出回答。

---

## 6. 创建信贷审批表单

### 6.1 表单字段设计

信贷审批表单需要以下字段：

| 字段Key | 字段名称 | 组件类型 | 必填 | 说明 |
|---------|---------|---------|------|------|
| applicantName | 申请人姓名 | TEXT | 是 | 借款申请人 |
| idCardNo | 身份证号 | TEXT | 是 | 18位身份证号 |
| loanAmount | 贷款金额 | AMOUNT | 是 | 万元，用于风险评分 |
| loanPurpose | 贷款用途 | SELECT | 是 | 个人消费/经营周转/购房/购车/教育 |
| loanTerm | 贷款期限 | NUMBER | 是 | 月，用于风险评分 |
| annualIncome | 年收入 | AMOUNT | 是 | 万元，用于还款能力评估 |
| creditScore | 信用评分 | NUMBER | 是 | 外部征信评分 300-850 |
| hasCollateral | 是否有抵押 | SELECT | 是 | 是/否 |
| companyName | 工作单位 | TEXT | 是 | 用于还款能力评估 |
| contactPhone | 联系电话 | TEXT | 是 | 手机号 |

### 6.2 通过 API 创建表单定义

```bash
curl -X POST http://localhost:8080/api/forms \
  -H "Content-Type: application/json" \
  -d '{
    "formCode": "CREDIT_APPLY_FORM",
    "formName": "信贷申请表单",
    "category": "信贷",
    "description": "个人信贷审批申请表单，用于AI风险评估和人工审批",
    "schemaJson": "{\"fields\":[{\"id\":\"applicantName\",\"fieldKey\":\"applicantName\",\"label\":\"申请人姓名\",\"componentType\":\"TEXT\",\"required\":true,\"span\":12,\"placeholder\":\"请输入申请人姓名\"},{\"id\":\"idCardNo\",\"fieldKey\":\"idCardNo\",\"label\":\"身份证号\",\"componentType\":\"TEXT\",\"required\":true,\"span\":12,\"placeholder\":\"请输入18位身份证号\"},{\"id\":\"loanAmount\",\"fieldKey\":\"loanAmount\",\"label\":\"贷款金额(万元)\",\"componentType\":\"AMOUNT\",\"required\":true,\"span\":12,\"placeholder\":\"例如 50\"},{\"id\":\"loanPurpose\",\"fieldKey\":\"loanPurpose\",\"label\":\"贷款用途\",\"componentType\":\"SELECT\",\"required\":true,\"span\":12,\"placeholder\":\"请选择贷款用途\",\"options\":[{\"label\":\"个人消费\",\"value\":\"个人消费\"},{\"label\":\"经营周转\",\"value\":\"经营周转\"},{\"label\":\"购房\",\"value\":\"购房\"},{\"label\":\"购车\",\"value\":\"购车\"},{\"label\":\"教育\",\"value\":\"教育\"}]},{\"id\":\"loanTerm\",\"fieldKey\":\"loanTerm\",\"label\":\"贷款期限(月)\",\"componentType\":\"NUMBER\",\"required\":true,\"span\":12,\"placeholder\":\"例如 12\"},{\"id\":\"annualIncome\",\"fieldKey\":\"annualIncome\",\"label\":\"年收入(万元)\",\"componentType\":\"AMOUNT\",\"required\":true,\"span\":12,\"placeholder\":\"例如 30\"},{\"id\":\"creditScore\",\"fieldKey\":\"creditScore\",\"label\":\"征信评分\",\"componentType\":\"NUMBER\",\"required\":true,\"span\":12,\"placeholder\":\"300-850\"},{\"id\":\"hasCollateral\",\"fieldKey\":\"hasCollateral\",\"label\":\"是否有抵押\",\"componentType\":\"SELECT\",\"required\":true,\"span\":12,\"placeholder\":\"请选择\",\"options\":[{\"label\":\"是\",\"value\":\"是\"},{\"label\":\"否\",\"value\":\"否\"}]},{\"id\":\"companyName\",\"fieldKey\":\"companyName\",\"label\":\"工作单位\",\"componentType\":\"TEXT\",\"required\":true,\"span\":12,\"placeholder\":\"请输入工作单位\"},{\"id\":\"contactPhone\",\"fieldKey\":\"contactPhone\",\"label\":\"联系电话\",\"componentType\":\"TEXT\",\"required\":true,\"span\":12,\"placeholder\":\"请输入手机号\"}]}"
  }'
```

### 6.3 发布表单

```bash
# 假设创建返回的 formId=2
curl -X POST http://localhost:8080/api/forms/2/publish
```

---

## 7. 设计信贷审批工作流

### 7.1 工作流设计说明

信贷审批流程设计如下：

```
                    ┌──────────────────┐
                    │     START        │
                    │    (开始节点)     │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │  AI_RISK_CHECK   │
                    │  (AI 风险检测)    │
                    │  RAG检索制度文档  │
                    │  大模型评分       │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │    CONDITION     │
                    │  (风险等级路由)   │
                    └──┬────┬────┬─────┘
                       │    │    │
              riskLevel│    │riskLevel  riskLevel
              =="LOW"  │    │=="MEDIUM" =="HIGH"
                       │    │    │
                       ▼    ▼    ▼
              ┌────────┐ ┌──────────┐ ┌──────────────┐
              │ NOTIFY │ │ APPROVAL │ │  APPROVAL    │
              │(AI自动 │ │(风控经理  │ │  (信贷委员会  │
              │ 通过)  │ │  审批)   │ │   审批)       │
              └───┬────┘ └────┬─────┘ └──────┬───────┘
                  │           │              │
                  └───────────┴──────────────┘
                              │
                              ▼
                     ┌──────────────────┐
                     │     NOTIFY       │
                     │   (结果通知)      │
                     └────────┬─────────┘
                              │
                              ▼
                     ┌──────────────────┐
                     │       END        │
                     └──────────────────┘
```

**节点说明**：

| 节点ID | 类型 | 名称 | 配置说明 |
|--------|------|------|---------|
| start | START | 开始 | 绑定表单 CREDIT_APPLY_FORM |
| ai_risk | AI_RISK_CHECK | AI 信贷风险检测 | threshold=70, autoApproveMaxScore=30, aiStrategy="信贷制度风险评分策略" |
| risk_gateway | CONDITION | 风险等级路由 | 根据 riskLevel 分流 |
| risk_manager_approve | APPROVAL | 风控经理审批 | assigneeType=ROLE, assigneeValue=MANAGER（中风险走 lisi） |
| credit_committee_approve | APPROVAL | 信贷委员会审批 | assigneeType=ROLE, assigneeValue=FINANCE（高风险走 finance） |
| notify_done | NOTIFY | 结果通知 | 通知发起人 |
| end | END | 结束 | - |

**条件路由规则**：

| 边 | 条件 | 目标 |
|----|------|------|
| e_low_auto | `riskLevel == "LOW"` | notify_done（AI 自动通过，跳过人工审批） |
| e_medium | `riskLevel == "MEDIUM"` | risk_manager_approve（风控经理审批） |
| e_high | `riskLevel == "HIGH"` | credit_committee_approve（信贷委员会审批） |

### 7.2 通过 API 创建工作流定义

```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "code": "CREDIT_APPROVAL",
    "name": "信贷审批流程",
    "description": "个人信贷审批流程：支持 AI/RAG 风险评分、低风险自动审批、中高风险人工审批和全程审计",
    "formJson": "{\"fields\":[{\"key\":\"applicantName\",\"label\":\"申请人姓名\",\"type\":\"TEXT\",\"required\":true},{\"key\":\"idCardNo\",\"label\":\"身份证号\",\"type\":\"TEXT\",\"required\":true},{\"key\":\"loanAmount\",\"label\":\"贷款金额(万元)\",\"type\":\"AMOUNT\",\"required\":true},{\"key\":\"loanPurpose\",\"label\":\"贷款用途\",\"type\":\"SELECT\",\"required\":true},{\"key\":\"loanTerm\",\"label\":\"贷款期限(月)\",\"type\":\"NUMBER\",\"required\":true},{\"key\":\"annualIncome\",\"label\":\"年收入(万元)\",\"type\":\"AMOUNT\",\"required\":true},{\"key\":\"creditScore\",\"label\":\"征信评分\",\"type\":\"NUMBER\",\"required\":true},{\"key\":\"hasCollateral\",\"label\":\"是否有抵押\",\"type\":\"SELECT\",\"required\":true},{\"key\":\"companyName\",\"label\":\"工作单位\",\"type\":\"TEXT\",\"required\":true},{\"key\":\"contactPhone\",\"label\":\"联系电话\",\"type\":\"TEXT\",\"required\":true}]}",
    "definitionJson": "{\"nodes\":[{\"id\":\"start\",\"nodeType\":\"START\",\"name\":\"开始\",\"config\":{\"formCode\":\"CREDIT_APPLY_FORM\"}},{\"id\":\"ai_risk\",\"nodeType\":\"AI_RISK_CHECK\",\"name\":\"AI 信贷风险检测\",\"config\":{\"formCode\":\"CREDIT_APPLY_FORM\",\"aiStrategy\":\"信贷制度风险评分策略\",\"threshold\":70,\"autoApproveMaxScore\":30,\"autoApproveActor\":\"ai_approver\",\"highRiskReceivers\":\"finance,admin\"}},{\"id\":\"risk_gateway\",\"nodeType\":\"CONDITION\",\"name\":\"风险等级路由\",\"config\":{\"formCode\":\"CREDIT_APPLY_FORM\",\"defaultRoute\":\"risk_manager_approve\"}},{\"id\":\"risk_manager_approve\",\"nodeType\":\"APPROVAL\",\"name\":\"风控经理审批\",\"config\":{\"formCode\":\"CREDIT_APPLY_FORM\",\"assigneeType\":\"ROLE\",\"assigneeValue\":\"MANAGER\"}},{\"id\":\"credit_committee_approve\",\"nodeType\":\"APPROVAL\",\"name\":\"信贷委员会审批\",\"config\":{\"formCode\":\"CREDIT_APPLY_FORM\",\"assigneeType\":\"ROLE\",\"assigneeValue\":\"FINANCE\"}},{\"id\":\"notify_done\",\"nodeType\":\"NOTIFY\",\"name\":\"结果通知\",\"config\":{\"receivers\":\"starter\",\"title\":\"信贷审批结果通知\",\"content\":\"您的信贷申请 {title} 已完成审批，风险等级：{riskLevel}，评分：{riskScore}。\"}},{\"id\":\"end\",\"nodeType\":\"END\",\"name\":\"结束\",\"config\":{}}],\"edges\":[{\"id\":\"e_start_ai\",\"source\":\"start\",\"target\":\"ai_risk\"},{\"id\":\"e_ai_gateway\",\"source\":\"ai_risk\",\"target\":\"risk_gateway\"},{\"id\":\"e_low_auto\",\"source\":\"risk_gateway\",\"target\":\"notify_done\",\"condition\":\"riskLevel == \\\"LOW\\\"\",\"label\":\"低风险AI自动通过\"},{\"id\":\"e_medium\",\"source\":\"risk_gateway\",\"target\":\"risk_manager_approve\",\"condition\":\"riskLevel == \\\"MEDIUM\\\"\",\"label\":\"中风险风控经理审批\"},{\"id\":\"e_high\",\"source\":\"risk_gateway\",\"target\":\"credit_committee_approve\",\"condition\":\"riskLevel == \\\"HIGH\\\"\",\"label\":\"高风险信贷委员会审批\"},{\"id\":\"e_manager_notify\",\"source\":\"risk_manager_approve\",\"target\":\"notify_done\"},{\"id\":\"e_committee_notify\",\"source\":\"credit_committee_approve\",\"target\":\"notify_done\"},{\"id\":\"e_notify_end\",\"source\":\"notify_done\",\"target\":\"end\"}]}"
  }'
```

### 7.3 发布工作流

```bash
# 假设创建返回的 workflowId=2
curl -X POST http://localhost:8080/api/workflows/2/publish
```

### 7.4 在工作流设计器中验证（可选）

打开前端 → 流程设计 → 选择"信贷审批流程"，可以在可视化画布中查看和编辑节点和连线。

---

## 8. 发起低风险审批申请（AI 自动审批）

### 8.1 场景说明

构造一个低风险申请：小额（10万）、短期（6个月）、高征信评分（800+）、有抵押、年收入远大于贷款金额。AI 应该给出低风险评分（≤30分），触发自动审批。

### 8.2 发起申请

```bash
curl -X POST http://localhost:8080/api/workflows/2/start \
  -H "Content-Type: application/json" \
  -d '{
    "starter": "zhangsan",
    "title": "张三个人消费贷款10万元",
    "businessData": {
      "applicantName": "张三",
      "idCardNo": "110101199001011234",
      "loanAmount": 10,
      "loanPurpose": "个人消费",
      "loanTerm": 6,
      "annualIncome": 35,
      "creditScore": 820,
      "hasCollateral": "是",
      "companyName": "字节跳动科技有限公司",
      "contactPhone": "13800138001"
    }
  }'
```

### 8.3 预期结果

返回的实例 JSON 中：

```json
{
  "id": 1,
  "definitionCode": "CREDIT_APPROVAL",
  "status": "COMPLETED",
  "riskLevel": "LOW",
  "riskScore": 25,
  "riskReason": "贷款金额10万元较小，贷款期限6个月短，征信评分820分优秀，有抵押担保，年收入35万元远超贷款金额，还款能力强，综合评估为低风险。",
  "aiSuggestion": "建议自动审批通过，贷款金额在合理范围内，申请人还款能力充足。",
  "currentNodeName": "流程正常结束。"
}
```

### 8.4 验证 — 低风险申请数据落库

```bash
# 1. 查看流程实例
curl -s http://localhost:8080/api/workflow-instances/1 | python3 -m json.tool

# 2. 查看实例动作日志（应看到 START → AI_RISK_CHECK → AI_AUTO_APPROVED → COMPLETED）
curl -s http://localhost:8080/api/workflow-instances/1/logs | python3 -m json.tool

# 3. 查看 AI 审计日志
curl -s http://localhost:8080/api/ai-audit/logs?instanceId=1 | python3 -m json.tool

# 4. 查看通知
curl -s "http://localhost:8080/api/notifications?recipient=zhangsan" | python3 -m json.tool

# 5. 直接查数据库
docker exec -it flowmind-postgres psql -U flowmind -d flowmind -c "
SELECT id, definition_code, title, status, risk_level, risk_score, risk_reason
FROM workflow_instance
WHERE starter = 'zhangsan';
"

docker exec -it flowmind-postgres psql -U flowmind -d flowmind -c "
SELECT id, action, actor, comment, created_at
FROM fm_workflow_action_log
WHERE instance_id = 1
ORDER BY created_at;
"
```

### 8.5 前端验证

打开前端 → 运行总览 → 实例追踪，可以看到实例记录，流程状态为"已完成"。

---

## 9. 发起高风险审批申请（切换身份走人工审批流程）

### 9.1 场景说明

构造一个高风险申请：大额（200万）、长期（36个月）、低征信评分（550）、无抵押、年收入不足以覆盖贷款金额。AI 应该给出高风险评分（≥70分），触发人工审批流程。

### 9.2 发起申请

```bash
curl -X POST http://localhost:8080/api/workflows/2/start \
  -H "Content-Type: application/json" \
  -d '{
    "starter": "zhangsan",
    "title": "张三经营周转贷款200万元",
    "businessData": {
      "applicantName": "张三",
      "idCardNo": "110101199001011234",
      "loanAmount": 200,
      "loanPurpose": "经营周转",
      "loanTerm": 36,
      "annualIncome": 25,
      "creditScore": 550,
      "hasCollateral": "否",
      "companyName": "张三杂货铺",
      "contactPhone": "13800138001"
    }
  }'
```

### 9.3 预期结果

返回的实例 JSON 中：

```json
{
  "id": 2,
  "definitionCode": "CREDIT_APPROVAL",
  "status": "RUNNING",
  "riskLevel": "HIGH",
  "riskScore": 85,
  "riskReason": "贷款金额200万元较大，贷款期限36个月长，征信评分550分偏低，无抵押担保，年收入25万元远低于贷款金额，还款能力不足，综合评估为高风险。",
  "aiSuggestion": "建议人工复核，贷款金额较大且无抵押，需风控和信贷委员会双重审批。",
  "currentNodeId": "credit_committee_approve",
  "currentNodeName": "信贷委员会审批"
}
```

### 9.4 步骤1：风控经理（李四 lisi）审批

**查询李四的待办任务**：

```bash
curl -s "http://localhost:8080/api/workflow-tasks/todo?assignee=lisi" | python3 -m json.tool
```

**李四审批通过**：

```bash
# 假设 taskId=1
curl -X POST http://localhost:8080/api/workflow-tasks/1/complete \
  -H "Content-Type: application/json" \
  -d '{
    "assignee": "lisi",
    "action": "APPROVED",
    "comment": "经营周转需求合理，虽然风险较高但建议进入下一级审批。"
  }'
```

### 9.5 步骤2：切换身份为信贷委员会（王五 finance）审批

**查询王五的待办任务**：

```bash
curl -s "http://localhost:8080/api/workflow-tasks/todo?assignee=finance" | python3 -m json.tool
```

**王五审批通过**：

```bash
# 假设 taskId=2
curl -X POST http://localhost:8080/api/workflow-tasks/2/complete \
  -H "Content-Type: application/json" \
  -d '{
    "assignee": "finance",
    "action": "APPROVED",
    "comment": "经信贷委员会审核，同意发放贷款，建议追加第三方担保。"
  }'
```

### 9.6 验证 — 高风险申请数据落库

```bash
# 1. 查看流程实例（应变为 COMPLETED）
curl -s http://localhost:8080/api/workflow-instances/2 | python3 -m json.tool

# 2. 查看完整动作日志
docker exec -it flowmind-postgres psql -U flowmind -d flowmind -c "
SELECT id, action, actor, node_name, comment, created_at
FROM fm_workflow_action_log
WHERE instance_id = 2
ORDER BY created_at;
"
```

预期日志序列：

```
ENTER_NODE        → "开始"
AI_RISK_CHECK     → "AI 信贷风险检测" (评分85，高风险)
AI_HUMAN_REVIEW_REQUIRED → "AI 判断需要人工复核"
ENTER_NODE        → "风险等级路由"
CREATE_TASK       → "风控经理审批" (分配给 lisi)
ENTER_NODE        → "风控经理审批"
APPROVED          → lisi 审批通过
CREATE_TASK       → "信贷委员会审批" (分配给 finance)
ENTER_NODE        → "信贷委员会审批"
APPROVED          → finance 审批通过
COMPLETED         → "流程正常结束"
```

```bash
# 3. 查看任务表
docker exec -it flowmind-postgres psql -U flowmind -d flowmind -c "
SELECT id, node_name, assignee, status, action, comment, created_at, completed_at
FROM workflow_task
WHERE instance_id = 2
ORDER BY created_at;
"

# 4. 查看通知
docker exec -it flowmind-postgres psql -U flowmind -d flowmind -c "
SELECT recipient, title, content, notification_type, created_at
FROM fm_notification_message
WHERE instance_id = 2
ORDER BY created_at;
"
```

### 9.7 前端验证（切换身份查看）

**作为申请人（张三）**：
- 前端 → 审批 → 发起审批 → 可以看到自己的申请记录
- 可以看到高风险申请的审批进度

**作为审批人（李四）**：
- 前端 → 审批 → 待审批 → 切换到 lisi 身份
- 可以看到待审批任务

**作为审批人（王五）**：
- 前端 → 审批 → 待审批 → 切换到 finance 身份
- 可以看到待审批任务

---

## 10. 数据库落库验证

### 10.1 完整数据表清单

执行以下 SQL 验证所有数据已落库：

```bash
docker exec -it flowmind-postgres psql -U flowmind -d flowmind << 'SQL'
-- 1. 用户表
SELECT 'sys_user' AS table_name, count(*) AS rows FROM sys_user;

-- 2. 角色表
SELECT 'sys_role' AS table_name, count(*) AS rows FROM sys_role;

-- 3. 表单定义表
SELECT 'fm_form_definition' AS table_name, count(*) AS rows FROM fm_form_definition;

-- 4. 表单版本表
SELECT 'form_version' AS table_name, count(*) AS rows FROM fm_form_version;

-- 5. 字段定义表
SELECT 'fm_field_definition' AS table_name, count(*) AS rows FROM fm_field_definition;

-- 6. 工作流定义表
SELECT 'workflow_definition' AS table_name, count(*) AS rows FROM workflow_definition;

-- 7. 工作流版本表
SELECT 'fm_workflow_version' AS table_name, count(*) AS rows FROM fm_workflow_version;

-- 8. 工作流实例表
SELECT 'workflow_instance' AS table_name, count(*) AS rows FROM workflow_instance;

-- 9. 工作流任务表
SELECT 'workflow_task' AS table_name, count(*) AS rows FROM workflow_task;

-- 10. 工作流动作日志表
SELECT 'fm_workflow_action_log' AS table_name, count(*) AS rows FROM fm_workflow_action_log;

-- 11. AI 审计日志表
SELECT 'fm_ai_audit_log' AS table_name, count(*) AS rows FROM fm_ai_audit_log;

-- 12. 通知消息表
SELECT 'fm_notification_message' AS table_name, count(*) AS rows FROM fm_notification_message;

-- 13. 知识库文档表
SELECT 'fm_knowledge_document' AS table_name, count(*) AS rows FROM fm_knowledge_document;

-- 14. 知识库块表
SELECT 'fm_knowledge_chunk' AS table_name, count(*) AS rows FROM fm_knowledge_chunk;
SQL
```

### 10.2 两条申请的数据对比

```bash
docker exec -it flowmind-postgres psql -U flowmind -d flowmind -c "
SELECT id, title, status, risk_level, risk_score, starter, created_at, completed_at
FROM workflow_instance
ORDER BY id;
"
```

预期输出：

```
 id |          title           |  status   | risk_level | risk_score | starter  |     created_at      |    completed_at
----+--------------------------+-----------+------------+------------+----------+---------------------+---------------------
  1 | 张三个人消费贷款10万元    | COMPLETED | LOW        |         25 | zhangsan | 2026-06-22 10:00:00 | 2026-06-22 10:00:05
  2 | 张三经营周转贷款200万元  | COMPLETED | HIGH       |         85 | zhangsan | 2026-06-22 10:05:00 | 2026-06-22 10:10:00
```

### 10.3 两条申请的任务对比

```bash
docker exec -it flowmind-postgres psql -U flowmind -d flowmind -c "
SELECT wt.id, wi.title, wt.node_name, wt.assignee, wt.status, wt.action, wt.comment
FROM workflow_task wt
JOIN workflow_instance wi ON wt.instance_id = wi.id
ORDER BY wt.id;
"
```

预期输出：

```
 id |          title           |    node_name     | assignee |  status  |  action   |              comment
----+--------------------------+------------------+----------+----------+-----------+-----------------------------------
  1 | 张三经营周转贷款200万元  | 风控经理审批      | lisi     | APPROVED | APPROVED  | 经营周转需求合理，虽然风险较高但建议进入下一级审批。
  2 | 张三经营周转贷款200万元  | 信贷委员会审批    | finance  | APPROVED | APPROVED  | 经信贷委员会审核，同意发放贷款，建议追加第三方担保。
```

> 注意：低风险申请（ID=1）不会产生 workflow_task 记录，因为 AI 自动审批通过，不创建人工待办任务。

---

## 11. 附录：模拟的信贷制度文档

### 11.1 文档1：信贷审批管理制度

将此内容保存为 `docs/信贷审批管理制度.md`：

```markdown
# 信贷审批管理制度

## 第一章 总则

**第一条** 为规范本行信贷审批管理，防范信贷风险，根据《商业银行法》和监管要求，制定本制度。

**第二条** 本制度适用于本行所有个人信贷业务的审批管理。

## 第二章 审批权限

**第三条** 信贷审批实行分级授权管理：

1. 贷款金额在 30 万元（含）以下的个人消费贷款，可由 AI 自动审批系统直接审批通过。
2. 贷款金额在 30 万元至 100 万元（含）的，需风控经理审批。
3. 贷款金额在 100 万元以上的，需信贷委员会审批。

## 第三章 风险评分标准

**第四条** 信贷风险评分采用百分制，综合考虑以下因素：

### 1. 贷款金额（权重 30%）
- 10万元以下：10分
- 10-30万元：8分
- 30-100万元：5分
- 100-200万元：3分
- 200万元以上：1分

### 2. 贷款期限（权重 15%）
- 6个月以内：10分
- 6-12个月：8分
- 12-24个月：5分
- 24-36个月：3分
- 36个月以上：1分

### 3. 征信评分（权重 25%）
- 800分以上：10分
- 700-800分：8分
- 600-700分：5分
- 500-600分：3分
- 500分以下：1分

### 4. 抵押担保（权重 15%）
- 有足值抵押：10分
- 有部分抵押：6分
- 无抵押：2分

### 5. 还款能力（权重 15%）
- 年收入/贷款金额 ≥ 3：10分
- 年收入/贷款金额 ≥ 1.5：7分
- 年收入/贷款金额 ≥ 0.5：4分
- 年收入/贷款金额 < 0.5：1分

**第五条** 风险等级划分：
- 综合评分 ≥ 70 分：低风险（LOW）
- 综合评分 40-69 分：中风险（MEDIUM）
- 综合评分 < 40 分：高风险（HIGH）

## 第四章 审批流程

**第六条** 低风险申请：AI 自动审批通过，系统自动发送通知。

**第七条** 中风险申请：由风控经理进行人工审批，审批通过后发放贷款。

**第八条** 高风险申请：由信贷委员会集体审批，需2/3以上委员同意方可发放贷款。

## 第五章 附则

**第九条** 本制度自发布之日起施行。
```

### 11.2 文档2：信贷风险评分标准

将此内容保存为 `docs/信贷风险评分标准.md`：

```markdown
# 信贷风险评分标准

## 一、概述

本标准用于量化评估个人信贷申请的风险等级，作为 AI 自动审批和人工审批的决策依据。

## 二、评分维度

### 维度1：申请人信用状况（25分）

| 征信评分 | 得分 |
|---------|------|
| 800-850 | 25 |
| 700-799 | 20 |
| 600-699 | 15 |
| 500-599 | 8  |
| 300-499 | 3  |

**否决条件**：征信评分低于 500 分且无抵押的，直接评为高风险。

### 维度2：还款能力（25分）

| 年收入/贷款金额比例 | 得分 |
|-------------------|------|
| ≥ 3.0             | 25   |
| 2.0-2.9           | 20   |
| 1.0-1.9           | 12   |
| 0.5-0.9           | 6    |
| < 0.5             | 2    |

### 维度3：贷款特征（25分）

| 评分项 | 高分条件 | 低分条件 |
|--------|---------|---------|
| 贷款金额 | ≤30万: 15分 | >200万: 3分 |
| 贷款期限 | ≤6月: 10分 | >36月: 2分 |

### 维度4：贷款用途（15分）

| 用途 | 得分 |
|------|------|
| 教育 | 15 |
| 购房 | 12 |
| 购车 | 10 |
| 个人消费 | 8  |
| 经营周转 | 5  |

### 维度5：抵押担保（10分）

| 抵押情况 | 得分 |
|---------|------|
| 足值抵押 | 10 |
| 部分抵押 | 6  |
| 无抵押   | 2  |

## 三、风险等级判定

- 总分 ≥ 70 分：**低风险（LOW）** — AI 自动审批通过
- 总分 40-69 分：**中风险（MEDIUM）** — 需风控经理审批
- 总分 < 40 分：**高风险（HIGH）** — 需信贷委员会审批

## 四、特殊规则

1. **一票否决**：征信评分 < 500 且无抵押 → 直接高风险
2. **快速通道**：征信评分 ≥ 800 且贷款金额 ≤ 30万 且有抵押 → 直接低风险
3. **灰名单**：贷款用途为"经营周转"且无抵押 → 风险评分额外扣 10 分
```

---

## 附录A：API 快速参考

### 用户相关
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/users` | GET | 查询所有用户 |
| `/api/users/{id}` | GET | 查询单个用户 |
| `/api/roles` | GET | 查询所有角色 |

### 表单相关
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/forms` | GET | 查询所有表单 |
| `/api/forms` | POST | 创建表单 |
| `/api/forms/{id}` | GET | 查询单个表单 |
| `/api/forms/{id}/publish` | POST | 发布表单 |
| `/api/fields` | GET | 查询所有字段定义 |

### 工作流定义相关
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/workflows` | GET | 查询所有流程定义 |
| `/api/workflows` | POST | 创建流程定义 |
| `/api/workflows/{id}` | GET | 查询单个流程定义 |
| `/api/workflows/{id}/publish` | POST | 发布流程定义 |
| `/api/workflows/{id}/start` | POST | 发起流程实例 |

### 工作流实例相关
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/workflow-instances` | GET | 查询所有实例 |
| `/api/workflow-instances/{id}` | GET | 查询单个实例 |
| `/api/workflow-instances/{id}/logs` | GET | 查询实例动作日志 |
| `/api/workflow-instances/my` | GET | 我的申请列表 |

### 工作流任务相关
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/workflow-tasks/todo` | GET | 查询待办任务 |
| `/api/workflow-tasks/done` | GET | 查询已办任务 |
| `/api/workflow-tasks/{id}/complete` | POST | 完成审批任务 |

### 知识库相关
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/knowledge/documents` | GET | 查询文档列表 |
| `/api/knowledge/documents/upload` | POST | 上传文档 |
| `/api/rag/query` | POST | RAG 检索 |

### AI 审计相关
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai-audit/logs` | GET | 查询 AI 审计日志 |

---

## 附录B：问题排查

| 问题 | 排查步骤 |
|------|---------|
| 后端启动失败 | 检查 PostgreSQL 是否启动：`docker ps \| grep postgres` |
| AI 审批不生效 | 确认 `DASHSCOPE_API_KEY` 环境变量已设置 |
| 文档上传失败 | 检查 Qdrant 是否启动：`docker ps \| grep qdrant` |
| 前端页面空白 | 检查后端是否启动：`curl http://localhost:8080/api/health` |
| 流程实例状态一直是 RUNNING | 检查是否有待办任务未处理：`/api/workflow-tasks/todo?assignee=xxx` |
| 条件路由不生效 | 检查 riskLevel 字段值是否正确，注意大小写敏感（LOW/MEDIUM/HIGH） |