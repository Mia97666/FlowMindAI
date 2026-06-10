# ARCHITECTURE.md — 麻将计分 小程序架构设计

> 版本：1.0 | 创建：2026-06-08 | 目标：快速上线，稳定迭代

---

## 一、技术选型与决策

### 1.1 核心决策矩阵

| 决策项 | 选择 | 理由 |
|--------|------|------|
| **开发方式** | 原生 WXML/WXSS/JS | 现有 HTML 代码结构良好，原生适配最快、性能最优、审核通过率最高 |
| **UI 组件库** | TDesign Miniprogram | 腾讯出品，60+ 组件开箱即用；Switch/Button/Dialog/Avatar 等组件可大幅加速开发 |
| **后端服务** | 腾讯云 CloudBase（云开发） | 免运维、自带微信登录集成、数据库 + 云函数 + 存储一体；省去后端开发周期 |
| **数据库** | CloudBase 文档数据库（NoSQL） | Schema-less 灵活迭代，适合产品早期快速调整；数据模型简单（文档嵌套） |
| **状态管理** | `app.globalData` + 页面级 `this.data` | 项目状态简单，无需引入 MobX/Redux 增加包体积和复杂度 |
| **包管理** | npm + 微信开发者工具构建 | TDesign 组件库必须通过 npm 引入 |
| **主题方案** | CSS 变量 `page { --var }` + `app.wxss` 全局注入 | 与现有 DESIGN.md 变量系统完全对应，切换零性能损耗 |

### 1.2 为什么不选 Taro/uni-app

- Taro/uni-app 编译后代码量更大，**包体积更难控制**（主包 2MB 限制）
- 现有代码是纯 vanilla JS，不依赖 React/Vue，**不需要跨平台能力**
- 原生开发**审核通过率最高**，避免框架层的潜在合规问题
- **快速上线**是第一目标，原生路径最短

### 1.3 为什么选 CloudBase

- **10 分钟内搭建后端**：数据库 + 云函数 + 认证全托管
- **微信登录零代码集成**：`wx.cloud` 直接获取 OPENID，无需手动调用 `wx.login` → 服务端换 token 流程
- **免域名备案**：cloudbase.net 域名已白名单，开发阶段无需备案
- **弹性伸缩**：初期可免费，DAU 增长后自动扩容

---

## 二、项目结构

```
mahjong-miniapp/
├── app.js                          # App 入口：生命周期、全局状态、主题初始化
├── app.json                        # 页面路由、窗口配置、Tab Bar、权限声明
├── app.wxss                        # 全局样式：CSS 变量、基础组件样式、双主题
├── project.config.json             # 项目配置（appid、miniprogramRoot、npm 设置）
├── sitemap.json                    # 微信搜索索引配置
├── package.json                    # npm 依赖（tdesign-miniprogram）
│
├── pages/                          # 页面（全在主包，总 6 页，预估 < 500KB）
│   ├── index/                      # 首页（创建牌局 + 二维码 + 固定小组）
│   │   ├── index.js                #   页面逻辑 + 二维码轮询
│   │   ├── index.json              #   组件引用 + 页面配置
│   │   ├── index.wxml              #   页面模板
│   │   └── index.wxss              #   页面样式
│   │
│   ├── scoring/                    # 计分页（对局 Tab + 计分 Tab）
│   │   ├── scoring.js              #   计分核心逻辑（输赢计算、收盘）
│   │   ├── scoring.json
│   │   ├── scoring.wxml
│   │   └── scoring.wxss
│   │
│   ├── games/                      # 牌局列表（进行中 / 已结束）
│   │   ├── games.js
│   │   ├── games.json
│   │   ├── games.wxml
│   │   └── games.wxss
│   │
│   ├── profile/                    # 我的（个人中心 + 菜单入口）
│   │   ├── profile.js
│   │   ├── profile.json
│   │   ├── profile.wxml
│   │   └── profile.wxss
│   │
│   ├── groups/                     # 固定小组管理
│   │   ├── groups.js
│   │   ├── groups.json
│   │   ├── groups.wxml
│   │   └── groups.wxss
│   │
│   ├── stats/                      # 个人战绩
│   │   ├── stats.js
│   │   ├── stats.json
│   │   ├── stats.wxml
│   │   └── stats.wxss
│   │
│   └── scoreboard/                 # 已结束牌局只读详情
│       ├── scoreboard.js
│       ├── scoreboard.json
│       ├── scoreboard.wxml
│       └── scoreboard.wxss
│
├── components/                     # 可复用自定义组件
│   ├── player-avatar/              # 玩家头像（圆形 + 颜色 + 冠军徽章）
│   ├── qr-card/                    # 二维码卡片容器
│   ├── round-history/              # 对局历史记录卡片
│   ├── win-lose-radio/             # 赢/输圆形单选按钮组
│   ├── score-input-row/            # 计分输入行（头像 + 胜负 + 分数 + 重置）
│   ├── game-card/                  # 牌局卡片（列表用）
│   ├── multiplier-control/         # 倍率开关 + 输入框
│   ├── group-list-item/            # 固定小组列表项
│   ├── custom-tab-bar/             # 自定义底部 Tab Bar
│   └── theme-toggle/               # 三选一主题切换按钮组
│
├── utils/                          # 工具模块
│   ├── request.js                  # 统一请求封装（CloudBase 调用 + REST fallback）
│   ├── auth.js                     # 登录态管理（微信登录 → CloudBase 认证）
│   ├── storage.js                  # 本地存储封装（Token、主题偏好、缓存）
│   ├── theme.js                    # 主题系统（检测系统偏好、切换、持久化）
│   ├── calculator.js               # 计分引擎（输赢计算、总和校验、倍率应用）
│   └── constants.js                # 常量（玩家颜色、默认倍率、错误码等）
│
├── services/                       # 业务服务层（封装 CloudBase 调用）
│   ├── room.js                     # 牌局 CRUD + 状态轮询 + 分数提交
│   ├── group.js                    # 固定小组 CRUD
│   ├── user.js                     # 用户信息管理
│   └── qrcode.js                   # 小程序码生成（云函数调用微信 API）
│
├── cloudfunctions/                 # CloudBase 云函数
│   ├── generateQRCode/             # 生成小程序码（调用 wxacode.getUnlimited）
│   ├── getOpenId/                  # 获取用户 OpenID
│   └── dataCleanup/                # 定时清理过期数据（可选）
│
└── images/                         # 本地图标资源（Tab 图标等，尽量用 SVG 减少体积）
    ├── tab-home.png
    ├── tab-games.png
    └── tab-profile.png
```

### 2.1 包体积预估

| 模块 | 预估大小 | 说明 |
|------|---------|------|
| TDesign 核心组件 | ~300KB | 仅引入实际使用的组件（Button、Switch、Dialog、Avatar、Input、Tab、Tabs、Toast、Empty） |
| 页面代码（7 页） | ~100KB | JS + WXML + WXSS |
| 自定义组件（10 个） | ~80KB | 通用组件 |
| utils + services | ~30KB | 工具库 |
| 云函数 | ~10KB | 3 个轻量云函数 |
| 图片资源 | ~30KB | Tab 图标 |
| **主包总计** | **~550KB** | 远低于 2MB 限制，无需分包 |

---

## 三、核心页面架构

### 3.1 页面路由设计

```
Tab Bar 页面（3个，始终在栈底）:
├── pages/index/index      → 首页
├── pages/games/games       → 牌局列表
└── pages/profile/profile   → 我的

子页面（通过 navigateTo 推入）:
├── pages/scoring/scoring   → 计分页（参数：roomId）
├── pages/groups/groups     → 固定小组管理
├── pages/stats/stats       → 个人战绩
└── pages/scoreboard/scoreboard → 已结束牌局详情（参数：roomId）
```

### 3.2 首页（index）架构

```
┌──────────────────────────────────┐
│  Status Bar (系统)                │
│  Hero: 麻将计分 + 副标题          │
├──────────────────────────────────┤
│  [扫码玩家头像区]                 │  ← 轮询更新
│  ┌─────────────────────┐         │
│  │   QR Code Card      │         │  ← 创建后生成
│  │   160×160            │         │
│  │   "微信扫一扫加入"   │         │
│  └─────────────────────┘         │
│  [台板 Switch] [创建新牌局 Btn]    │
├──────────────────────────────────┤
│  固定小组   [管理 ›]              │
│  ┌ 周末麻将局  3人 ●进行中 ┐     │  ← 从 CloudBase 加载
│  ┌ 邻居牌友圈  4人 ○空闲   ┐     │
├──────────────────────────────────┤
│  [首页]    [牌局]    [我的]       │  ← 自定义 Tab Bar
└──────────────────────────────────┘
```

**关键数据流**：
```
1. 进入首页 → onShow() → 从 CloudBase 加载固定小组列表
2. 创建牌局 → 云函数生成小程序码 → 展示二维码 → 启动 2s 轮询
3. 有人扫码 → CloudBase 实时更新 → 轮询检测 → 更新头像区
4. 点击固定小组 → 检查是否有进行中牌局 → 有则进计分页 / 无则开新局
```

### 3.3 计分页（scoring）架构

```
┌──────────────────────────────────┐
│  ← 返回    周末麻将局             │
├──────────────────────────────────┤
│  [对局 🔒]  [计分]               │  ← 自定义双 Tab
├──────────────────────────────────┤
│  ┌─── 对局 Tab ─────────────┐    │
│  │ [倍率 Switch] [1.0 ×]    │    │
│  │ 总分排行                  │    │
│  │ 👑 東  +15               │    │
│  │ #2 南   +5               │    │
│  │ #3 西   -8               │    │
│  │ #4 北  -12               │    │
│  │ 对局历史                  │    │
│  │ 第1局 14:30  +8 -4 +3 -7 │    │
│  │ [📸 截图分享]             │    │
│  └──────────────────────────┘    │
│  ┌─── 计分 Tab ─────────────┐    │
│  │ 玩家 │胜负  │得分         │    │
│  │ 🀄東  ◎赢 ○输 [8] ↺    │    │
│  │ 🀄南  ○赢 ◎输 [4] ↺    │    │
│  │ [✓ 确认本局分数]          │    │
│  │ [收盘] [我们是固定搭子]    │    │
│  └──────────────────────────┘    │
└──────────────────────────────────┘
```

**对局 Tab 加锁逻辑**：
```javascript
// 状态：rounds.length === 0 → 对局 Tab 锁定
// 第一局确认后 → 解锁对局 Tab
hasRounds = this.data.room.rounds.length > 0
```

### 3.4 牌局列表（games）架构

```
┌──────────────────────────────────┐
│  牌局                             │
│  [进行中]  [已结束]               │  ← TDesign Tabs
├──────────────────────────────────┤
│  ┌ 周末麻将局        🟢 进行中   ┐│
│  │ [東] [南] [西] [北]          ││
│  │ +15   +5   -8  -12           ││
│  │ 开始于 5/15 14:00 · 共4局    ││
│  └──────────────────────────────┘│
│  ┌ 邻居牌友圈         🏁 已结束  ┐│
│  │ [東] [南] [西] [北]          ││
│  │ +20  -10   -5   -5           ││
│  │ 结束于 4/20 18:30 · 共6局    ││
│  └──────────────────────────────┘│
├──────────────────────────────────┤
│  [首页]    [牌局]    [我的]       │
└──────────────────────────────────┘
```

---

## 四、数据架构

### 4.1 CloudBase 数据库集合设计

#### rooms（牌局集合）
```javascript
{
  _id: "room_xxx",                    // CloudBase 自动生成
  _openid: "user_openid_xxx",        // 创建者
  name: "周末麻将局",                  // 牌局名称
  groupId: "group_xxx" || null,      // 关联固定小组
  status: "active" | "closed",      // 状态
  multiplier: 1.0,                   // 倍率
  multiplierEnabled: false,          // 倍率开关
  tableFee: false,                   // 台板费
  players: [                         // 参与者（固定4人）
    {
      userId: "openid_xxx",
      name: "東",
      avatarColor: "#DC2626",
      avatarText: "東",
      totalScore: 15
    }
  ],
  rounds: [                          // 对局记录（嵌套数组）
    {
      roundNumber: 1,
      time: "2026-06-08T14:30:00Z",
      scores: {
        "openid_1": 8,
        "openid_2": -4,
        "openid_3": 3,
        "openid_4": -7
      }
    }
  ],
  createdAt: "2026-06-08T14:00:00Z",
  closedAt: null,
  qrCodeScene: "room_xxx",           // 小程序码场景值
  qrCodeUrl: "cloud://xxx.png"       // 云存储中小程序码 URL
}
```

#### groups（固定小组集合）
```javascript
{
  _id: "group_xxx",
  _openid: "user_openid_xxx",        // 创建者
  name: "周末麻将局",
  memberIds: ["openid_1", "openid_2", "openid_3", "openid_4"],
  memberAvatars: ["東", "南", "西", "北"],
  avatarColors: ["#DC2626", "#15803D", "#2563EB", "#D97706"],
  hasActiveGame: false,
  activeRoomId: null,
  createdAt: "2026-05-15T10:00:00Z"
}
```

#### users（用户集合）
```javascript
{
  _id: "openid_xxx",                 // 以 openId 作为 _id
  nickName: "麻将爱好者",
  avatarText: "我",
  createdAt: "2026-06-01T08:00:00Z",
  stats: {                           // 个人统计（云函数异步更新）
    totalGames: 0,
    totalWins: 0,
    maxScore: 0
  }
}
```

### 4.2 本地存储设计

| Key | 内容 | 用途 |
|-----|------|------|
| `theme_preference` | `'system'` \| `'light'` \| `'dark'` | 主题偏好持久化 |
| `user_profile` | `{ nickName, avatarText }` | 用户信息本地缓存 |
| `token` | JWT access token | 如需自建后端时的认证 |
| `cached_groups` | 固定小组列表 | 减少首屏加载时间 |

### 4.3 数据同步策略

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  小程序端     │────▶│  CloudBase   │────▶│  其他玩家     │
│  (创建者)    │     │  数据库       │     │  小程序端     │
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │                      │
       │  1. 创建牌局       │                      │
       │  2. 生成小程序码    │                      │
       │  3. 轮询 /rooms    │◀─────────────────────│
       │     status 变化     │   4. 扫码加入         │
       │  5. 提交分数        │                      │
       │  6. 收盘            │                      │
       │                    │   7. 查询历史         │
```

**实时性方案**：
- **二维码扫码加入**：使用 `wx.cloud.callFunction` 实时更新数据库中的 players 字段。首页每 2 秒查询一次 `rooms` 集合中玩家的变化。
- **计分过程**：每局结束后一次性提交（POST round），不需要实时同步。所有玩家在自己的设备上独立操作，通过 CloudBase 数据库保持一致。
- **牌局列表**：进入 games 页面时从 CloudBase 拉取最新数据即可。

---

## 五、组件树与复用策略

### 5.1 HTML → WXML 映射关系

| HTML 中的模式 | Mini Program 对应 | 说明 |
|--------------|-------------------|------|
| `document.querySelectorAll('.page')` + `classList` | `wx.navigateTo` / `wx.switchTab` | 页面切换用路由而非 DOM 隐藏 |
| `localStorage` | `wx.getStorageSync/setStorageSync` | API 完全兼容 |
| `onclick="handler()"` | `bindtap="handler"` | 事件绑定语法不同 |
| `innerHTML = "..."` | `wx:for="{{list}}"` + `setData` | 用数据驱动而非直接操作 DOM |
| `document.getElementById` | `this.selectComponent('#id')` | 组件引用 |
| CSS `:root` / `[data-theme]` | `page { --var }` / `page[data-theme]` | 微信小程序中 page 是根元素 |
| `position: fixed` overlay | `wx;` `fixed` 无效 → 用 `catchtouchmove` 阻止穿透 | 小程序 fixed 定位有坑 |
| `document.documentElement.setAttribute` | `this.setData({ theme })` → WXML `data-theme="{{theme}}"` | 主题切换方式不同 |
| `@keyframes` 动画 | 微信支持 CSS animation，或 `wx.createAnimation` | CSS 动画基本兼容 |

### 5.2 组件依赖关系

```
pages/index
  ├── qr-card           (二维码容器)
  ├── player-avatar      (扫码玩家头像)
  ├── group-list-item    (固定小组条目)
  └── custom-tab-bar     (底部导航)

pages/scoring
  ├── multiplier-control (倍率控制)
  ├── player-avatar      (排行榜头像)
  ├── round-history      (对局历史)
  ├── win-lose-radio     (胜负选择)
  ├── score-input-row    (分数输入行)
  └── custom-tab-bar

pages/games
  ├── game-card          (牌局卡片)
  └── custom-tab-bar

pages/profile
  ├── theme-toggle       (主题切换)
  └── custom-tab-bar

pages/scoreboard
  ├── player-avatar
  └── round-history
```

---

## 六、主题系统实现方案

### 6.1 CSS 变量迁移

小程序中 CSS 变量需定义在 `page` 选择器上（而非 `:root`）：

```css
/* app.wxss */
page {
  /* 暗色主题变量（默认） */
  --color-bg: #0F172A;
  --color-card: #192134;
  --color-card-fg: #F1F5F9;
  --color-muted: #1E293B;
  --color-muted-fg: #94A3B8;
  --color-border: rgba(255,255,255,0.08);
  --color-surface: #192134;
  --color-overlay: rgba(0,0,0,0.65);

  /* 固定色 */
  --color-primary: #15803D;
  --color-on-primary: #FFFFFF;
  --color-accent: #D97706;
  --color-success: #15803D;
  --color-danger: #DC2626;

  /* 间距系统 */
  --space-xs: 4px;  --space-sm: 8px;  --space-md: 12px;
  --space-lg: 16px; --space-xl: 20px; --space-2xl: 24px;

  /* 圆角 */
  --radius-sm: 8px;  --radius-md: 12px;
  --radius-lg: 16px; --radius-xl: 20px; --radius-pill: 26px;
}

/* 亮色主题覆盖 */
page[data-theme="light"] {
  --color-bg: #F8FAFC;
  --color-card: #FFFFFF;
  --color-card-fg: #0F172A;
  --color-muted: #F1F5F9;
  --color-muted-fg: #64748B;
  --color-border: #E2E8F0;
  --color-surface: #FFFFFF;
  --color-overlay: rgba(15,23,42,0.35);
}
```

### 6.2 主题切换流程

```javascript
// utils/theme.js
const THEME_KEY = 'theme_preference';

function getSystemTheme() {
  const sys = wx.getSystemInfoSync();
  return sys.theme || 'light'; // 微信基础库 2.20+ 支持
}

function resolveTheme(preference) {
  if (preference === 'system') return getSystemTheme();
  return preference;
}

function applyTheme(theme) {
  // 更新所有页面的 data-theme
  const pages = getCurrentPages();
  pages.forEach(page => {
    page.setData({ theme: theme });
  });
}

function initTheme() {
  const preference = wx.getStorageSync(THEME_KEY) || 'light';
  const resolved = resolveTheme(preference);
  applyTheme(resolved);
  return { preference, resolved };
}
```

### 6.3 WXML 中应用主题

```wxml
<!-- 每个页面根元素绑定 data-theme -->
<view class="page-container" data-theme="{{theme}}">
  <!-- 页面内容 -->
</view>
```

---

## 七、关键微信 API 使用方案

### 7.1 小程序码生成

```
流程：
1. 首页点击「创建新牌局」
2. 前端调用云函数 generateQRCode
3. 云函数使用 cloud.openapi.wxacode.getUnlimited({
     scene: "roomId=xxx",
     page: "pages/scoring/scoring"
   })
4. 返回小程序码 Buffer → 上传到云存储
5. 返回 fileID 给前端展示
```

```javascript
// cloudfunctions/generateQRCode/index.js
exports.main = async (event) => {
  const { roomId } = event;
  const result = await cloud.openapi.wxacode.getUnlimited({
    scene: `roomId=${roomId}`,
    page: 'pages/scoring/scoring',
    width: 280,
    checkPath: false
  });
  const fileID = await cloud.uploadFile({
    cloudPath: `qrcodes/${roomId}.png`,
    fileContent: result.buffer
  });
  return { fileID: fileID.fileID };
};
```

### 7.2 扫码加入牌局

```javascript
// app.js - 扫码进入处理
App({
  onLaunch(options) {
    // 通过小程序码进入
    if (options.query.scene) {
      const scene = decodeURIComponent(options.query.scene);
      // scene 格式: "roomId=xxx"
      const roomId = scene.split('=')[1];
      this.globalData.pendingRoomId = roomId;
    }
  },
  
  onShow(options) {
    if (this.globalData.pendingRoomId) {
      this.joinRoom(this.globalData.pendingRoomId);
      this.globalData.pendingRoomId = null;
    }
  }
});
```

### 7.3 截图分享

小程序不支持 HTML 的 canvas 截图方案，改用微信原生：

```javascript
// 方案一：wx.showShareImageMenu（推荐，微信 8.0+）
// 将计分结果渲染为一张图片 → 长按触发系统分享
wx.showShareImageMenu({
  path: 'cloud://score-screenshot.png' // 云存储中的分享图
});

// 方案二：Canvas 2D 绘制分享图
const query = wx.createSelectorQuery();
query.select('#shareCanvas').fields({ node: true, size: true });
// ...绘制分数排行榜为图片
```

### 7.4 分享小程序

```javascript
// 在需要的页面添加
Page({
  onShareAppMessage() {
    return {
      title: '来一起麻将计分吧！',
      path: '/pages/index/index',
      imageUrl: '/images/share-cover.png'
    };
  },
  onShareTimeline() {
    return {
      title: '麻将计分 - 好友对局，轻松计分',
      query: '',
      imageUrl: '/images/share-cover.png'
    };
  }
});
```

### 7.5 用户登录

```javascript
// utils/auth.js
async function login() {
  // CloudBase 方式（推荐，最简单）
  await wx.cloud.callFunction({
    name: 'getOpenId'
  }).then(res => {
    const openId = res.result.openId;
    wx.setStorageSync('openId', openId);
    return openId;
  });

  // 同步用户信息到数据库
  const db = wx.cloud.database();
  const user = await db.collection('users').doc(openId).get();
  if (!user.data) {
    await db.collection('users').add({
      data: {
        _id: openId,
        nickName: '麻将爱好者',
        avatarText: '我',
        createdAt: new Date()
      }
    });
  }
}
```

---

## 八、性能优化策略

### 8.1 setData 优化（关键！）

小程序中每次 `setData` 都会触发 JS → Native 的跨线程通信，需要严格控制：

```javascript
// ❌ 错误：多次 setData
this.setData({ 'players[0].score': 10 });
this.setData({ 'players[1].score': -5 });
this.setData({ 'players[2].score': 3 });
this.setData({ 'players[3].score': -8 });

// ✅ 正确：一次 setData，合并所有更新
this.setData({
  'players[0].score': 10,
  'players[1].score': -5,
  'players[2].score': 3,
  'players[3].score': -8,
  currentRoundNum: 2
});

// ✅ 计分页对局历史：仅更新新增的局，不解构整个 rounds 数组
const newIndex = room.rounds.length - 1;
this.setData({
  [`room.rounds[${newIndex}]`]: newRound,
  [`room.players`]: updatedPlayers,
  currentRoundNum: room.rounds.length + 1
});
```

### 8.2 图片优化

- 头像使用纯色背景 + 文字（无需加载图片），0 网络请求
- 小程序码使用云存储 CDN，自动 WebP 格式
- Tab Bar 图标使用本地 PNG（3 个图标，每个 < 5KB）

### 8.3 长列表优化

牌局列表（games 页面）和历史记录使用分页：

```javascript
// 分页加载参数
const PAGE_SIZE = 20;

// 下拉加载更多
onReachBottom() {
  if (this.data.hasMore) {
    this.loadGames(this.data.currentPage + 1);
  }
}
```

### 8.4 启动优化

```
优先级：
1. 仅 app.js 中执行必要的初始化（主题、登录）
2. 固定小组列表在首页 onShow 时异步加载
3. 非当前 Tab 页面延迟初始化（onFirstShow 模式）
4. TDesign 组件按需引入（仅引入使用的 9 个组件）
```

---

## 九、安全与合规

### 9.1 数据权限控制

```javascript
// CloudBase 数据库权限规则
{
  "rooms": {
    ".read": "doc._openid == auth.openid",     // 只能读自己创建的
    ".write": "doc._openid == auth.openid"     // 只能写自己创建的
  },
  "groups": {
    ".read": "doc._openid == auth.openid",
    ".write": "doc._openid == auth.openid"
  },
  "users": {
    ".read": "doc._id == auth.openid",         // 只能读自己的
    ".write": "doc._id == auth.openid"
  }
}
```

### 9.2 隐私合规清单

| 项目 | 状态 | 说明 |
|------|------|------|
| 用户昵称 | 无需授权 | 使用自定义昵称而非 `wx.getUserProfile` |
| 头像 | 无需授权 | 使用文字头像（纯色圆形 + 首字），不获取微信头像 |
| 用户 OpenID | 静默获取 | CloudBase 云函数获取，用户无感知 |
| 隐私政策 | 需配置 | 小程序后台 → 设置 → 隐私保护指引 |
| 数据删除 | 待实现 | 用户的页面中提供「清除数据」入口 |

### 9.3 合规优势

本小程序**不索取任何微信敏感权限**：
- 不需要用户头像（文字头像替代）
- 不需要地理定位
- 不需要手机号
- 不需要相册/相机权限
- 不需要通讯录

这对微信审核通过率极大利好。

---

## 十、开发路线图（快速上线版）

### 第一阶段：MVP 核心功能（Week 1-2）

```
Day 1-2  ├── 项目初始化
          │   ├── 创建小程序项目，配置 app.json
          │   ├── 引入 TDesign Miniprogram
          │   ├── 搭建 app.wxss 主题系统
          │   └── 搭建 utils/ 基础模块（request, auth, theme, storage）
          │
Day 3-4  ├── CloudBase 后端搭建
          │   ├── 创建 CloudBase 环境
          │   ├── 设计数据库集合（rooms, groups, users）
          │   ├── 编写云函数（getOpenId, generateQRCode）
          │   └── 配置数据库权限
          │
Day 5-7  ├── 首页开发
          │   ├── 页面布局 + 主题适配
          │   ├── 二维码生成 + 轮询逻辑
          │   ├── 固定小组列表
          │   └── 创建牌局流程
          │
Day 8-10 ├── 计分页开发（核心！）
          │   ├── 双 Tab 切换 + 加锁逻辑
          │   ├── 对局 Tab：排行 + 历史 + 倍率
          │   ├── 计分 Tab：输赢选择 + 分数输入 + 自动计算
          │   ├── 确认一局 + 收盘逻辑
          │   └── 固定小组保存
          │
Day 11-12├── 牌局列表 + 详情
          │   ├── 进行中/已结束列表
          │   ├── 已结束详情（只读）
          │   └── 从列表恢复到计分页
          │
Day 13-14├── 我的 + 小组管理 + 战绩
          │   ├── 个人中心菜单
          │   ├── 主题切换（跟随/浅色/深色）
          │   ├── 固定小组管理（改名/删除）
          │   └── 个人战绩统计
```

### 第二阶段：上线准备（Week 3）

```
Day 15-16 ├── 联调测试
           │   ├── iOS + Android 真机测试
           │   ├── 扫码加入流程完整走通
           │   ├── 多设备数据同步验证
           │   └── 主题切换全页面测试
           │
Day 17-18 ├── 性能优化
           │   ├── setData 调用优化
           │   ├── 包体积检查（确保 < 2MB）
           │   ├── 启动时间优化
           │   └── 微信开发者工具 Audits 评分
           │
Day 19-21 ├── 审核提交
           │   ├── 配置隐私政策
           │   ├── 准备审核材料
           │   ├── 提交审核
           │   └── 处理审核反馈
```

### 第三阶段：灰度与迭代（Week 4+）

```
Week 4   ├── 小范围灰度（邀请 20-50 位核心用户）
         ├── 收集反馈
         ├── Bug 修复
         └── 全量发布
```

---

## 十一、关键风险与应对

| 风险 | 概率 | 影响 | 应对方案 |
|------|------|------|----------|
| 微信审核不通过 | 中 | 高 | 不索取敏感权限；隐私政策完善；类目选「工具-信息查询」 |
| 小程序码生成超时 | 低 | 中 | 加 loading 状态；超时重试；降级展示文字邀请码 |
| 多人同时计分数据冲突 | 低 | 中 | 设计上由一人操作（房主确认），CloudBase 乐观锁 |
| TDesign 组件兼容性问题 | 低 | 低 | 仅使用基础组件（Button/Switch/Dialog），这些最稳定 |
| 包体积超标 | 极低 | 高 | 当前预估 550KB，有充足余量；按需引入 TDesign |
| CloudBase 费用超预期 | 低 | 低 | 初期免费额度足够（5GB 存储、10 万次调用/月） |

---

## 十二、与现有 HTML 原型的差异总结

| 方面 | HTML 原型 | 微信小程序 |
|------|----------|-----------|
| 页面切换 | DOM 显示/隐藏 + `classList` | `wx.navigateTo` / `wx.switchTab` 路由 |
| 状态管理 | 全局 `STATE` 对象 + 手动 `renderXxx()` | `this.data` + `this.setData()` 响应式 |
| 数据持久化 | `localStorage`（单设备） | CloudBase（多设备同步） |
| 二维码 | HTML 占位 SVG | 真实小程序码（云函数调用微信 API） |
| 扫码加入 | 键盘模拟 `1/2/3` | 真实微信扫码 → scene 参数解析 |
| 截图分享 | HTML 模拟 Toast | `wx.showShareImageMenu` 真实分享 |
| 主题检测 | `matchMedia('prefers-color-scheme')` | `wx.getSystemInfoSync().theme` |
| 登录 | 无 | CloudBase 微信静默登录 |
| 用户隔离 | 无（单用户） | 基于 OpenID 的多用户数据隔离 |
| 多人协作 | 无 | CloudBase 数据库同步 |

---

> 📐 产品文档：[PRODUCT.md](./PRODUCT.md) | 🎨 设计规范：[DESIGN.md](./DESIGN.md) | 🚀 开发代码将基于此架构展开
