# PRODUCT.md — 麻将计分 小程序

> 版本：1.2 | 平台：微信小程序 | 最后更新：2026-06-08

---

## 一、产品概述

### 1.1 产品定位

「麻将计分」是一款面向麻将爱好者的轻量级计分工具小程序。核心解决线下麻将局中 **手动算分繁琐、对局记录易丢失、朋友间缺乏共享计分方式** 的痛点。通过微信小程序的能力，实现扫码快速组局、一键记录分数、自动汇总统计、截图分享战果。

### 1.2 目标用户

| 用户类型 | 场景描述 | 核心诉求 |
|----------|----------|----------|
| **发起者（房主）** | 组织麻将局的人，负责创建牌局 | 快速建局、生成二维码、管理固定小组 |
| **参与者** | 扫码加入牌局的好友 | 扫码即加入、查看自己的输赢、查看历史 |
| **围观者** | 不参与计分但需要回顾的牌友 | 查看已结束牌局的完整历史 |

### 1.3 核心价值

- **0 门槛建局**：一键生成二维码，好友微信扫码即加入
- **灵活计分**：支持自定义倍率（可小于 1），赢输自选 + 手动输入分数
- **全记录可追溯**：每局历史清晰展示，已收盘牌局永久可查
- **固定小组**：常用牌友一键组局，省去重复扫码

### 1.4 信息架构

```
Tab Bar（底部导航）
├── 首页      → 创建牌局 → 生成二维码 → 扫码加入 → 自动进入计分
├── 牌局      → 进行中牌局 / 已结束牌局 → 点击进入详情
└── 我的      → 个人战绩 / 固定小组管理 / 页面风格切换 / 邀请好友 / 关于

子页面（非 Tab）
├── 计分页     → 对局tab(总分排行+对局历史+截图分享) / 计分tab(输赢选择+分数输入+单分重置+确认+收盘+保存固定小组)
├── 固定小组管理 → 修改名称 / 批量删除 / 一键清空
├── 个人战绩   → 统计概览 + 历史牌局列表
└── 已结束详情  → 只读台板，展示完整对局历史
```

---

## 二、页面功能详述

### 2.1 首页（Tab - 首页）

| 项目 | 说明 |
|------|------|
| **路由** | `pages/index/index` |
| **布局** | 亮色/暗色双主题自适应 + 居中内容区 + 底部 Tab Bar |
| **顶部** | 标题「麻将计分」（28px Bold，亮色模式渐变绿色），副标题「好友对局，轻松计分」 |
| **参与者头像** | 扫码加入的用户头像横向排列在二维码上方 |
| **二维码区** | 200×240 卡片容器，内嵌二维码。有人扫码后卡片边框变为绿色提示 |
| **台板开关** | Switch 开关 + 「台板」标签，开启后计分时扣台板费（50分/人/局） |
| **主按钮** | 「创建新牌局」（绿色 #15803D，圆角 pill），不足2人时提示 |
| **固定小组列表** | 左上角「固定小组」标签，右上角小字「管理」按钮。列表项显示组名、进行中/空闲状态、成员头像。点击进入对应牌局 |
| **交互** | 创建牌局 → 生成二维码 → 扫码加入 → 头像出现 → 创建牌局 → 进入计分页（计分tab，对局tab锁定） |

### 2.2 计分页（子页面）

| 项目 | 说明 |
|------|------|
| **路由** | `pages/scoring/scoring?roomId={roomId}` |
| **布局** | 顶部小组名 + 对局/计分双Tab + 滚动内容区 |
| **顶部** | 小组名称（或「临时小组」） |

#### 对局 Tab
| 功能 | 说明 |
|------|------|
| **倍率控制** | Switch 开关 + 倍率数值输入（支持 <1，step 0.5） |
| **总分排行** | 按分数从高到低排列，冠军头像上方有 👑 徽章 |
| **对局历史** | 大卡片，顶部头像行 + 局数列表（局数/时间/各人分数） |
| **截图分享** | 底部按钮，生成分享图片 |

#### 计分 Tab
| 功能 | 说明 |
|------|------|
| **计分表格** | 名称列(头像) \| 胜负列(赢/输圆形单选) \| 得分列(输入框+单个重置按钮) |
| **确认按钮** | 确认本局 → 弹窗「第X局分数已统计」→ 跳到对局tab（解锁） |
| **收盘** | 金色按钮 → 输入倍率 → 展示总分 → 「截图分享」按钮 |
| **我们是固定搭子** | 仅临时小组显示 → 输入名称 → 保存为固定小组 → 首页联动出现 |

### 2.3 牌局（Tab - 牌局）

| 项目 | 说明 |
|------|------|
| **路由** | `pages/games/games` |
| **布局** | 页面标题 + 分段控制器 + 牌局卡片列表 + Tab Bar |
| **分段控制器** | 「进行中」（默认）+「已结束」，绿色/灰色切换 |
| **进行中卡片** | 展示：小组名（或「临时小组」）、4 个头像并排、头像下总分数、开始时间 |
| **已结束卡片** | 展示：小组名、4 个头像并排、头像下总分数、结束时间 |

### 2.4 固定小组管理页（子页面）

| 项目 | 说明 |
|------|------|
| **路由** | `pages/groups/groups` |
| **入口** | 我的 → 固定小组管理 / 首页固定小组区域「管理」按钮 |

#### 功能
| 功能 | 触发 | 行为 |
|------|------|------|
| **修改名称** | 点击「✎ 修改」 | 弹窗输入新名称 → 保存 |
| **批量删除** | 勾选 + 点击按钮 | 二次确认 → 删除选中 |
| **一键清空** | 点击红色按钮 | 二次确认 → 清空全部 |

### 2.5 我的页面（Tab - 我的）

| 项目 | 说明 |
|------|------|
| **路由** | `pages/profile/profile` |
| **布局** | 用户卡片 + 菜单列表 + Tab Bar |

#### 菜单项
| 菜单项 | 行为 |
|--------|------|
| **个人战绩** | 跳转个人战绩页（总局数/总胜场/胜率/最高分 + 历史列表） |
| **固定小组管理** | 跳转固定小组管理页 |
| **邀请好友** | 分享小程序 |
| **页面风格** | 三选一切换：跟随系统 / ☀️ 亮色 / 🌙 暗色 |
| **关于麻将计分** | 版本号、简介 |

---

## 三、数据模型

### 3.1 牌局（Room）

```typescript
interface Room {
  id: string;                    // UUID，牌局唯一标识
  name: string;                  // 牌局名称（固定小组名 或 "麻将计分"）
  groupId: string | null;        // 关联固定小组 ID，临时局为 null
  status: 'active' | 'closed';  // 牌局状态
  multiplier: number;            // 当前倍率（默认 1）
  multiplierEnabled: boolean;    // 倍率开关
  players: PlayerScore[];        // 参与者及其累计分数
  rounds: Round[];               // 对局历史
  createdAt: ISO8601;            // 创建时间
  closedAt: ISO8601 | null;      // 收盘时间
  createdBy: string;             // 创建者 openId
}
```

### 3.2 参与者分数（PlayerScore）

```typescript
interface PlayerScore {
  userId: string;                // 用户 openId
  avatarUrl: string;             // 微信头像 URL
  totalScore: number;            // 累计总分数
}
```

### 3.3 单局记录（Round）

```typescript
interface Round {
  roundNumber: number;           // 局数（从 1 开始自增）
  time: ISO8601;                 // 该局记录时间
  scores: {                      // 每位玩家本局分数
    [userId: string]: number;    // 正数=赢，负数=输
  };
}
```

### 3.4 固定小组（Group）

```typescript
interface Group {
  id: string;                    // UUID
  name: string;                  // 小组名称
  memberIds: string[];           // 成员 openId 列表
  memberAvatars: string[];       // 成员头像列表
  createdAt: ISO8601;            // 创建时间
  createdBy: string;             // 创建者 openId
}
```

### 3.5 用户（User）

```typescript
interface User {
  openId: string;                // 微信 openId
  nickName: string;              // 微信昵称
  avatarUrl: string;             // 微信头像
  createdAt: ISO8601;
}
```

---

## 四、后台服务 API

### 4.1 基础信息

| 项目 | 值 |
|------|-----|
| **Base URL** | `https://api.mahjong-scorer.cn/miniapp/v1` |
| **认证方式** | Bearer Token（JWT，通过微信 code 换取的 session token） |
| **Content-Type** | `application/json` |
| **HTTPS** | 强制（微信小程序要求） |

### 4.2 用户认证

#### POST /auth/login

```
说明：微信登录，用 code 换取 token
Request:  { code: string }          // wx.login() 返回的 code
Response: { accessToken: string, refreshToken: string, user: User }
```

#### POST /auth/refresh

```
说明：刷新过期的 access token
Request:  { refreshToken: string }
Response: { accessToken: string, refreshToken: string }
```

### 4.3 牌局管理

#### POST /rooms/create

```
说明：创建新牌局，返回二维码
Request:  { groupId?: string }     // 可选，固定小组 ID
Response: {
  roomId: string,
  qrCodeUrl: string,               // 小程序码图片 URL
  scene: string                    // 小程序码携带的场景值
}
```

#### GET /rooms/{roomId}

```
说明：获取牌局详情
Response: Room
```

#### GET /rooms/{roomId}/status

```
说明：轮询牌局状态（是否有新玩家加入）
Response: {
  status: 'waiting' | 'playing' | 'closed',
  players: PlayerScore[]           // 当前参与的玩家列表
}
```

#### POST /rooms/{roomId}/join

```
说明：扫码加入牌局
Request:  { scene: string }       // 小程序码场景值
Response: {
  roomId: string,
  isNewGroup: boolean             // 是否首次扫码此小组
}
```

#### POST /rooms/{roomId}/round

```
说明：提交一局分数。客户端收集所有 4 人输赢 + 分数后提交
Request: {
  round: {
    scores: { [userId]: number }, // 每人的正/负分数
  }
}
Response: {
  round: Round,                   // 新创建的局记录
  players: PlayerScore[]          // 更新后的累计分数
}
```

#### PUT /rooms/{roomId}/close

```
说明：收盘，结束牌局
Request:  {}
Response: { room: Room }          // status 变为 'closed'，含 closedAt
```

#### GET /rooms?status=active|closed

```
说明：获取用户的牌局列表（数据面板）
Query:    status=active|closed, page=1, size=20
Response: { items: Room[], total: number }
```

### 4.4 固定小组管理

#### POST /groups

```
说明：创建固定小组
Request:  { name: string, roomId: string }
Response: Group
```

#### GET /groups

```
说明：获取用户的所有固定小组
Response: { items: Group[], total: number }
```

#### DELETE /groups/batch

```
说明：批量删除固定小组
Request:  { ids: string[] }
Response: { deletedCount: number }
```

#### DELETE /groups/all

```
说明：一键清空所有固定小组
Request:  {}
Response: { deletedCount: number }
```

### 4.5 小程序码生成

#### POST /qrcode/generate

```
说明：生成带 scene 参数的小程序码（内部调用微信 API）
Request:  { scene: string, page?: string }
Response: { qrCodeUrl: string }           // 返回图片 URL
```

实现方式：服务端调用 `wxacode.getUnlimited` 生成小程序码，scene 参数携带 roomId。

### 4.6 错误码

| Code | 说明 |
|------|------|
| `1000` | 参数校验失败 |
| `1001` | Token 过期 |
| `1002` | Token 无效 |
| `2001` | 牌局不存在 |
| `2002` | 牌局已收盘，不可修改 |
| `2003` | 非牌局参与者 |
| `3001` | 小组不存在 |
| `3002` | 小组名称重复 |
| `4000` | 服务器内部错误 |

---

## 五、小程序技术方案

### 5.1 项目结构

```
miniapp/
├── app.js                          # App 生命周期、全局数据
├── app.json                        # 页面路由、窗口、Tab Bar 配置
├── app.wxss                        # 全局样式（CSS 变量定义）
├── project.config.json             # 项目配置
├── pages/
│   ├── index/                      # 首页
│   │   ├── index.js
│   │   ├── index.json
│   │   ├── index.wxml
│   │   └── index.wxss
│   ├── scoring/                    # 计分页
│   ├── data/                       # 数据面板
│   ├── profile/                    # 我的
│   ├── groups/                     # 固定小组管理
│   └── scoreboard/                 # 只读台板（已结束详情）
├── components/
│   ├── player-card/                # 玩家卡片（头像+标签+分数）
│   ├── round-history/              # 对局历史项
│   ├── multiplier-control/         # 倍率控制组件
│   ├── game-card/                  # 牌局卡片（数据面板用）
│   ├── tab-bar/                    # 底部 Pill Tab Bar
│   └── qr-code/                    # 二维码展示组件
├── utils/
│   ├── request.js                  # 统一请求封装（Token 管理、401 重试）
│   └── util.js                     # 工具函数
└── services/
    ├── room.js                     # 牌局相关 API
    ├── group.js                    # 小组相关 API
    └── auth.js                     # 登录相关 API
```

### 5.2 app.json 关键配置

```json
{
  "pages": [
    "pages/index/index",
    "pages/data/data",
    "pages/profile/profile",
    "pages/scoring/scoring",
    "pages/groups/groups",
    "pages/scoreboard/scoreboard"
  ],
  "window": {
    "navigationStyle": "custom",
    "backgroundTextStyle": "light",
    "backgroundColor": "#0F172A"
  },
  "tabBar": {
    "custom": true,
    "list": [
      { "pagePath": "pages/index/index", "text": "首页" },
      { "pagePath": "pages/data/data", "text": "数据" },
      { "pagePath": "pages/profile/profile", "text": "我的" }
    ]
  },
  "requiredPrivateInfos": [
    "getFuzzyLocation",
    "chooseAddress"
  ]
}
```

### 5.3 关键 API 使用

| 微信 API | 用途 | 调用时机 |
|----------|------|----------|
| `wx.login()` | 获取 code 用于换取 token | App.onLaunch |
| `wx.getUserProfile()` | 获取用户头像和昵称 | 首次授权 / 我的页面 |
| `wx.request()` | 所有 HTTP 请求 | 全局 |
| `wx.showShareImageMenu()` | 截图分享 | 计分页「截图分享」按钮 |
| `wx.showShareMenu()` | 分享小程序 | 我的 → 邀请好友 |
| `wx.onAppRoute()` | 监听页面路由 | App 级埋点 |
| `wx.getStorageSync()` / `wx.setStorageSync()` | Token 持久化 | 登录 / 启动时 |

### 5.4 性能注意事项

1. **小程序码轮询**：创建牌局后使用 2s 间隔轮询，5 分钟后超时停止
2. **setData 优化**：计分页的对局历史使用局部 setData（`'rounds[0].scores'`），避免全量更新
3. **包体积**：主包保持在 1.5MB 以内，所有页面在 pages/ 下不使用分包
4. **图片优化**：二维码使用微信 CDN 返回的 WebP 格式，头像使用 132px 裁剪版本
5. **长列表**：数据面板的对局列表超过 20 条时使用分页加载（`onReachBottom`）

### 5.5 小程序码场景值解析

用户扫码进入小程序时，在 `App.onLaunch` / `Page.onLoad` 中通过 `options.scene` 获取场景值：

```javascript
// app.js
App({
  onLaunch(options) {
    if (options.query.scene) {
      // 解码 scene（微信会对 scene 做 URL encode）
      const scene = decodeURIComponent(options.query.scene);
      // scene 格式: "roomId=xxx"
      const roomId = scene.split('=')[1];
      this.globalData.pendingRoomId = roomId;
    }
  }
});
```

---

## 六、用户流程

### 6.1 创建牌局流程

```
[首页] → 点击「创建新牌局」
  → 请求 POST /rooms/create
  → 展示二维码 + 「微信扫一扫加入」
  → 开始轮询 GET /rooms/{roomId}/status
  → 检测到新玩家加入
    → 是首次扫码该小组？
      ├── 是 → 弹出「设为固定小组？」对话框
      │         ├── 点击是 → 输入/确认名称 → POST /groups → 进入计分页
      │         └── 点击否 → 进入计分页（标题显示「麻将计分」）
      └── 否 → 直接进入计分页
```

### 6.2 扫码加入流程

```
[微信扫一扫 小程序码]
  → 解析 scene 获取 roomId
  → POST /rooms/{roomId}/join
  → 进入计分页（与创建者同步看到牌局界面）
```

### 6.3 计分流程

```
[计分页]
  → 对每位玩家：点击标签选择「赢」或「输」
  → 输入分数（数字键盘）
  → 可选：开启倍率开关，设倍率值
  → 全部确认后自动记录一局
    → POST /rooms/{roomId}/round
    → 对局历史刷新
  → 继续下一局...
  → 点击「收盘」
    → PUT /rooms/{roomId}/close
    → 跳转数据面板「已结束」tab
```

### 6.4 查看历史流程

```
[数据面板]
  → 切换「进行中」或「已结束」
  → 点击牌局卡片
    → 进行中 → 进入计分页（可继续操作）
    → 已结束 → 进入只读台板（不可修改，无收盘按钮）
```

---

## 七、非功能需求

### 7.1 性能

| 指标 | 目标值 |
|------|--------|
| 小程序启动时间 | < 1.5s（4G 网络，中端 Android） |
| 页面切换耗时 | < 300ms |
| API 响应时间 | P95 < 500ms |
| 小程序码生成 | < 2s |
| 包体积（主包） | < 1.5MB |

### 7.2 兼容性

- **微信版本**：≥ 8.0.0
- **基础库版本**：≥ 2.20.0
- **iOS**：≥ 14.0
- **Android**：≥ 8.0

### 7.3 安全性

- 所有 API 请求使用 HTTPS + JWT Bearer Token
- Token 有效期 2 小时，通过 refresh token 续期
- 用户数据隔离：API 层校验 openId，确保用户只能操作自己的数据
- 微信登录 code 仅使用一次，防止重放攻击
- 所有用户输入进行 XSS 过滤

### 7.4 隐私合规

- 首次获取用户信息时弹出授权
- 隐私政策页面需在小程序后台配置
- 数据存储遵循《个人信息保护法》要求
- 用户可请求删除个人数据（通过设置页）

---

## 八、版本规划

### v1.0（已完成）
- ✅ 首页：创建牌局 + 二维码 + 台板开关 + 固定小组列表
- ✅ 计分页：对局tab(总分/历史) + 计分tab(输赢选择+分数输入+单分重置+确认)
- ✅ 牌局tab：进行中/已结束分段
- ✅ 固定小组管理：修改名称/批量删除/一键清空
- ✅ 我的页面：个人战绩/固定小组管理/页面风格切换/邀请好友/关于
- ✅ 个人战绩页面：总局数/总胜场/胜率/最高分 + 历史列表
- ✅ 亮色/暗色双主题（默认跟随系统，检测不到默认亮色）
- ✅ 计分tab锁定：第一局完成前对局tab不可见
- ✅ 收盘弹窗：最终成绩 + 截图分享按钮

### v1.1（当前版本 — 2026.06.08）
- ✅ 亮色视觉核心全面优化（#F8FAFC 背景、多层阴影、渐变标题）
- ✅ 页面过渡动画（pageIn 0.25s ease）
- ✅ 对局tab加锁机制（无对局时 🔒 锁定，完成第一局后解锁）
- ✅ 收盘流程优化（最终成绩弹窗优先「截图分享」按钮）
- ✅ 固定小组入口优化（首页直接显示 + 右上角管理按钮）
- ✅ 主题切换UI升级（三选一按钮：跟随/☀️/🌙）

### v1.2（计划）

- 自定义头像颜色/图案
- 对局历史导出（图片 / 文本）
- 排行榜（好友间总输赢统计）
- 语音播报每局结果
- 微信分享卡片可自定义封面
- 支持 3 人或 2 人麻将模式
- 订阅消息通知（收盘提醒、好友邀请）

---

> 📐 设计规范参见 [DESIGN.md](./DESIGN.md) | 🎨 UI 设计文件：[Ardot Design](https://ardot.tencent.com/file/690775576792497)
