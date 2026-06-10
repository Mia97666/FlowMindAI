# DESIGN.md — 麻将计分 小程序

> 基于 awesome-design-md 规范生成 | 参考风格：Vibrant & Block-based + Card & Board Game 色彩 | 支持亮色/暗色双主题

---

## 1. Visual Theme & Atmosphere

**品牌设计哲学**：传统麻将文化与现代数字体验的融合。以活力翠绿与金色点缀传递竞技感与仪式感。大块面布局、高对比度色彩、几何化圆角元素构建出年轻化的游戏计分工具。

**双主题策略**：默认跟随系统色彩偏好，检测不到时默认亮色。用户可在"我的"页面手动切换。亮色模式清新明快（Slate 灰白底 + 白卡片），暗色模式沉浸专注（Dark Navy 底 + 深色卡片）。

**暗色基调**：深色游戏风、高对比、圆润友好
**亮色基调**：清新扁平风、白底绿缀、轻盈透气

**核心视觉特征**：
- 双主题无缝切换（CSS 变量驱动）
- 高饱和翠绿主色 `#15803D` + 金色强调 `#D97706`
- 大块面卡片布局（Vibrant Block-based）
- 圆角饱满（avatars 圆形，cards 12-16px，buttons 24-28px）
- 无传统 1px 实线边框，用块面色差和间距区分层级

**光影质感**：暗色模式纯扁平 + 微阴影；亮色模式轻柔阴影 + 细边框

---

## 2. Color Palette & Roles

### 2.1 CSS 变量系统（双主题）

```css
:root {
  /* ===== 暗色主题（默认后备） ===== */
  --color-bg: #0F172A;
  --color-card: #192134;
  --color-card-fg: #FFFFFF;
  --color-muted: #1E293B;
  --color-muted-fg: #94A3B8;
  --color-border: rgba(255,255,255,0.08);
  --color-surface: #192134;
  --color-overlay: rgba(0,0,0,0.6);
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.3);
  --shadow-md: 0 4px 12px rgba(0,0,0,0.4);
  --shadow-lg: 0 8px 24px rgba(0,0,0,0.5);

  /* 固定色（不随主题变化） */
  --color-primary: #15803D;
  --color-on-primary: #FFFFFF;
  --color-secondary: #166534;
  --color-on-secondary: #FFFFFF;
  --color-accent: #D97706;
  --color-on-accent: #FFFFFF;
  --color-success: #15803D;
  --color-danger: #DC2626;
  --color-info: #94A3B8;
  --color-ring: #15803D;
}

/* 亮色主题 */
[data-theme="light"] {
  --color-bg: #F8FAFC;
  --color-card: #FFFFFF;
  --color-card-fg: #0F172A;
  --color-muted: #F1F5F9;
  --color-muted-fg: #64748B;
  --color-border: #E2E8F0;
  --color-surface: #FFFFFF;
  --color-overlay: rgba(15,23,42,0.4);
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.04), 0 1px 3px rgba(0,0,0,0.06);
  --shadow-md: 0 4px 6px rgba(0,0,0,0.04), 0 2px 16px rgba(0,0,0,0.06);
  --shadow-lg: 0 10px 25px rgba(0,0,0,0.06), 0 4px 10px rgba(0,0,0,0.04);
  --input-bg: #F1F5F9;
}
```

### 2.2 Primary Colors（固定，双主题共用）

| Token | HEX | CSS Variable | 用途 |
|-------|-----|--------------|------|
| Primary | `#15803D` | `--color-primary` | 主按钮、活跃态、赢分文字 |
| On Primary | `#FFFFFF` | `--color-on-primary` | 主色上的文字/图标 |
| Secondary | `#166534` | `--color-secondary` | 辅助元素、hover 变体 |
| On Secondary | `#FFFFFF` | `--color-on-secondary` | 辅助色上的文字 |

### 2.3 Accent / Interactive（固定）

| Token | HEX | CSS Variable | 用途 |
|-------|-----|--------------|------|
| Accent | `#D97706` | `--color-accent` | 强调色、收盘按钮、冠军徽章 |
| On Accent | `#FFFFFF` | `--color-on-accent` | 强调色上的文字 |

### 2.4 Neutral / Gray Scale（主题相关）

| Token | 暗色值 | 亮色值 | CSS Variable | 用途 |
|-------|--------|--------|--------------|------|
| Background | `#0F172A` | `#F8FAFC` | `--color-bg` | 页面背景 |
| Card | `#192134` | `#FFFFFF` | `--color-card` | 卡片/面板背景 |
| Card FG | `#FFFFFF` | `#0F172A` | `--color-card-fg` | 卡片内文字 |
| Muted | `#1E293B` | `#E2E8F0` | `--color-muted` | 输入框等次要底色 |
| Muted FG | `#94A3B8` | `#64748B` | `--color-muted-fg` | 次要文字、未选中态 |

### 2.5 Surface & Borders（主题相关）

| Token | 暗色值 | 亮色值 | CSS Variable | 用途 |
|-------|--------|--------|--------------|------|
| Border | `rgba(255,255,255,0.08)` | `#E2E8F0` | `--color-border` | 分割线、输入框描边 |
| Surface | `#192134` | `#FFFFFF` | `--color-surface` | 面板底色 |
| Overlay | `rgba(0,0,0,0.6)` | `rgba(0,0,0,0.3)` | `--color-overlay` | 遮罩层 |

### 2.6 Semantic Colors（固定）

| Token | HEX | CSS Variable | 用途 |
|-------|-----|--------------|------|
| Success | `#15803D` | `--color-success` | 赢分、正向数值 |
| Danger | `#DC2626` | `--color-danger` | 输分、删除、负向数值 |
| Warning | `#D97706` | `--color-warning` | 警告提示 |
| Info | `#64748B` | `--color-info` | 提示信息、未选态 |
| Ring | `#15803D` | `--color-ring` | 聚焦环 |

### 2.7 Shadow Colors（主题相关）

| Token | 暗色值 | 亮色值 | 用途 |
|-------|--------|--------|------|
| Shadow-sm | `rgba(0,0,0,0.3)` | `rgba(0,0,0,0.04), rgba(0,0,0,0.06)` | 卡片微阴影 |
| Shadow-md | `rgba(0,0,0,0.4)` | `rgba(0,0,0,0.04), rgba(0,0,0,0.06)` | 浮层/二维码卡片 |
| Shadow-lg | `rgba(0,0,0,0.5)` | `rgba(0,0,0,0.06), rgba(0,0,0,0.04)` | 模态框阴影 |

---

## 3. Typography Rules

### Font Family
- **Heading**: `Inter`, `PingFang SC`, `Microsoft YaHei`, system-ui, sans-serif
- **Body**: `Inter`, `PingFang SC`, `Microsoft YaHei`, sans-serif
- **Number/Mono**: `JetBrains Mono`, `SF Mono`, `Menlo`, monospace

### Type Scale

| Level | Size (px/rem) | Weight | Line Height | Letter Spacing | 用途 |
|-------|--------------|--------|-------------|----------------|------|
| Display Hero | 28 / 1.75rem | 800 | 1.2 | -0.5px | 首页标题 |
| Heading L | 22 / 1.375rem | 700 | 1.3 | 0 | 页面大标题 |
| Heading M | 18 / 1.125rem | 700 | 1.3 | 0 | 小组名、面板标题 |
| Heading S | 16 / 1rem | 700 | 1.4 | 0 | 导航栏标题 |
| Subheading | 15 / 0.9375rem | 600 | 1.4 | 0 | 区块标题 |
| Body L | 15 / 0.9375rem | 500 | 1.5 | 0 | 按钮文字、卡片标题 |
| Body | 14 / 0.875rem | 400 | 1.5 | 0 | 正文、标签 |
| Caption | 13 / 0.8125rem | 400 | 1.5 | 0 | 辅助信息 |
| Small | 12 / 0.75rem | 400 | 1.4 | 0 | 时间、元信息 |
| Nano | 11 / 0.6875rem | 500 | 1.3 | 0.5px | Tab 标签 |

**设计哲学**：Inter 字体提供清晰的中英文阅读体验，中文回退 PingFang SC。大号标题吸引注意力，小号辅助文字保持信息密度。分数使用等宽字体确保对齐。

---

## 4. Component Stylings

### Buttons

**Primary Button**（创建牌局、确认、收盘）
```css
.btn-primary {
  background: var(--color-primary);
  color: var(--color-on-primary);
  border-radius: 26px;
  padding: 13px 24px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  height: 50px;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-primary:active { opacity: 0.85; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
```

**Secondary Button**（截图分享、取消）
```css
.btn-secondary {
  background: var(--color-card);
  color: var(--color-card-fg);
  border: 1px solid var(--color-border);
  border-radius: 24px;
  padding: 12px 24px;
  font-size: 15px;
  font-weight: 500;
  height: 46px;
  cursor: pointer;
}
.btn-secondary:active { opacity: 0.8; }
```

**Ghost Button**（重置单个分数）
```css
.btn-ghost {
  background: transparent;
  color: var(--color-muted-fg);
  border: none;
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
}
.btn-ghost:hover { color: var(--color-card-fg); }
```

**Danger Button**（删除、清空）
```css
.btn-danger {
  background: var(--color-danger);
  color: #FFFFFF;
  border-radius: 16px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  border: none;
  height: 34px;
  cursor: pointer;
}
.btn-danger:active { opacity: 0.85; }
```

**Accent Button**（收盘）
```css
.btn-accent {
  background: var(--color-accent);
  color: #FFFFFF;
  border-radius: 26px;
  padding: 13px 24px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  height: 50px;
  cursor: pointer;
}
.btn-accent:active { opacity: 0.85; }
```

### Cards
```css
.card {
  background: var(--color-card);
  border-radius: 16px;
  padding: 16px;
  box-shadow: var(--shadow-sm);
}
.card-clickable:active { opacity: 0.8; }
```

### Inputs & Toggle
```css
.input-block {
  background: var(--color-muted);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 10px 14px;
  color: var(--color-card-fg);
  font-size: 14px;
  width: 100%;
  outline: none;
  transition: border-color 0.15s;
}
.input-block:focus {
  border-color: var(--color-ring);
  box-shadow: 0 0 0 3px rgba(21,128,61,0.15);
}
.input-block::placeholder { color: var(--color-muted-fg); }

/* Toggle Switch */
.toggle-track {
  width: 48px; height: 28px;
  border-radius: 14px;
  background: #94A3B8;
  transition: background 0.2s;
  cursor: pointer;
  position: relative;
}
.toggle-track.on { background: var(--color-primary); }
.toggle-thumb {
  width: 22px; height: 22px;
  border-radius: 11px;
  background: #FFFFFF;
  position: absolute; top: 3px; left: 3px;
  transition: left 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.toggle-track.on .toggle-thumb { left: 23px; }
```

### Navigation (Bottom Tab Bar)
```css
.tab-bar {
  background: var(--color-card);
  border-top: 1px solid var(--color-border);
  height: 64px;
  display: flex;
  padding-bottom: env(safe-area-inset-bottom, 0);
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: var(--color-muted-fg);
  font-size: 11px;
  cursor: pointer;
  transition: color 0.15s;
}
.tab-item.active { color: var(--color-primary); }
.tab-icon { width: 22px; height: 22px; }
```

### Segmented Control (Page-level Tabs)
```css
.segmented-control {
  display: flex;
  background: var(--color-muted);
  border-radius: 10px;
  padding: 3px;
  gap: 3px;
}
.seg-item {
  flex: 1;
  border-radius: 8px;
  padding: 8px 0;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-muted-fg);
  cursor: pointer;
  transition: all 0.15s;
}
.seg-item.active {
  background: var(--color-card);
  color: var(--color-card-fg);
  box-shadow: var(--shadow-sm);
}
```

### Badges / Tags
```css
.badge {
  display: inline-flex;
  align-items: center;
  border-radius: 10px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
}
.badge-win { background: var(--color-success); color: #FFFFFF; }
.badge-lose { background: transparent; color: var(--color-danger); border: 1px solid var(--color-danger); }
.badge-empty { background: transparent; color: var(--color-muted-fg); }
.badge-champion {
  background: var(--color-accent);
  color: #FFFFFF;
  border-radius: 50%;
  width: 20px; height: 20px;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px;
  position: absolute; top: -4px; right: -4px;
}
```

### Radio (Win/Lose Selector)
```css
.radio-group {
  display: flex;
  gap: 6px;
}
.radio-btn {
  width: 44px; height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px solid var(--color-border);
  background: transparent;
  color: var(--color-muted-fg);
  font-weight: 600;
  font-size: 13px;
  transition: all 0.15s;
}
.radio-btn.win.selected { background: var(--color-success); border-color: var(--color-success); color: #FFF; }
.radio-btn.lose.selected { background: var(--color-danger); border-color: var(--color-danger); color: #FFF; }
```

### Modals / Dialogs
```css
.modal-overlay {
  position: fixed; inset: 0;
  background: var(--color-overlay);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  z-index: 200;
  animation: fadeIn 0.2s ease;
}
.modal-content {
  background: var(--color-card);
  border-radius: 20px;
  padding: 24px;
  width: 300px;
  box-shadow: var(--shadow-lg);
  animation: scaleIn 0.2s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }
```

---

## 5. Layout Principles

### Spacing System
```css
/* 基于 4px 基数 */
--space-xs: 4px;
--space-sm: 8px;
--space-md: 12px;
--space-lg: 16px;
--space-xl: 20px;
--space-2xl: 24px;
--space-3xl: 32px;
--space-4xl: 48px;
```

### Container
- **viewport**: `max-width: 430px; margin: 0 auto`（居中模拟手机）
- **content-padding**: `16px`（左右内边距）
- **safe-area-bottom**: `env(safe-area-inset-bottom, 16px)`

### Page Structure
```
┌─────────────────────────┐
│  Status Bar (44px)      │
│  Page Header (48px)     │
├─────────────────────────┤
│                         │
│  Scrollable Content     │
│  (flex: 1, overflow-y)  │
│                         │
├─────────────────────────┤
│  Bottom Tab Bar (64px)  │
│  + safe-area-bottom     │
└─────────────────────────┘
```

### 留白哲学
充足留白 = 信息呼吸感。卡片间距统一 12-16px，内容区两侧留白 16px。底部为 Tab Bar 预留安全区。避免拥挤，每个卡片承载单一信息单元。亮色模式下留白感知更强，暗色模式下色块分区更明显。

---

## 6. Depth & Elevation

### Shadow System
```css
--shadow-xs: 0 1px 2px rgba(0,0,0,0.05);    /* 微卡片 */
--shadow-sm: 0 1px 3px rgba(0,0,0,0.1);      /* 普通卡片 */
--shadow-md: 0 4px 12px rgba(0,0,0,0.12);    /* 浮层 */
--shadow-lg: 0 8px 24px rgba(0,0,0,0.16);    /* 模态框 */
--shadow-2xl: 0 16px 48px rgba(0,0,0,0.2);   /* 全屏弹窗 */
```

### Surface Layers
| Layer | Z-index | 用途 |
|-------|---------|------|
| Background | 0 | 页面底色 |
| Surface | 10 | 卡片/面板 |
| Sticky | 50 | 吸顶导航 |
| Elevated | 100 | 浮层、下拉菜单 |
| Overlay | 200 | 模态框遮罩 |
| Modal | 210 | 模态框内容 |
| Toast | 300 | 提示信息 |

---

## 7. Do's and Don'ts

### Do's
1. ✅ 使用 CSS 变量 `var(--color-*)` 确保双主题自动适配
2. ✅ 主交互元素用 `#15803D`，强调操作用 `#D97706`
3. ✅ 赢分绿色、输分红色，保持视觉一致性
4. ✅ 卡片圆角 12-16px，按钮圆角 22-26px
5. ✅ 头像 44-56px 圆形，统一使用首字/emoji 占位
6. ✅ 操作按钮按重要程度分主次（绿 > 金 > 灰底白字 > 红底）
7. ✅ Tab Bar 3 个顶级标签，图标 22px + 标签 11px
8. ✅ 间距统一使用 4px 基数系统
9. ✅ 主题切换存储在 localStorage，同步到 `data-theme` 属性
10. ✅ 分数数字使用等宽字体保证对齐

### Don'ts
1. ❌ 不要硬编码色值，必须使用 CSS 变量
2. ❌ 不要在亮色模式使用纯黑 `#000` 文字
3. ❌ 不要在暗色模式使用纯白 `#FFF` 文字（用 `#F1F5F9`）
4. ❌ 不要用 1px 实线边框分隔卡片内容，用间距和色差
5. ❌ 不要混用不同字号的标题，全 App 内页面标题统一 16-18px
6. ❌ 不要在卡片内放置超过 5 个独立信息块
7. ❌ 不要对非关键操作使用 `#15803D`
8. ❌ 不要在不同页面使用不同的标题字号

---

## 8. Responsive Behavior

### Breakpoints
| 断点 | 宽度 | 目标设备 |
|------|------|----------|
| Mobile | `375px` (基准) | iPhone 6/7/8/SE |
| Mobile Wide | `414px` | iPhone 11/12/13/14 |
| Mobile Max | `430px` | iPhone 14 Pro Max |
| Tablet | `768px` | iPad Mini |

### Touch Targets
- 最小触摸区：`44px × 44px`
- 按钮推荐高度：`46-50px`
- 列表项推荐高度：`52-56px`

### 折叠策略
- 移动端：单列布局，满宽卡片，max-width 430px
- 平板端：430px 居中，两侧留暗色/亮色边距
- 头像行超过 4 人时缩小尺寸

### Font Scaling
- 基准 14px（body），rem 相对缩放
- 分数使用 vw 单位在小屏上自适应

---

## 9. Agent Prompt Guide

### Quick Reference

```
项目：麻将计分 小程序 Web App
风格：Vibrant Block-based Dual-Theme
主色：#15803D (Forest Green)
强调色：#D97706 (Gold)
暗色背景：#0F172A | 暗色卡片：#192134
亮色背景：#F1F5F9 | 亮色卡片：#FFFFFF
字体：Inter + PingFang SC
圆角：卡片 12-16px，按钮 22-26px
间距基数：4px
视口基准：375-430px 移动端
Tab Bar：3 tabs（首页/牌局/我的），高度 64px
主题：data-theme="light"|"dark"，默认跟随系统
```

### Component Prompts

**Prompt 1: 创建首页**
```
基于 DESIGN.md 生成麻将计分首页，双主题适配，包含：
- 顶部标题"麻将计分"（28px/800）+ 副标题
- 自动生成的二维码卡片（160×160）+ 扫码提示
- 参与者头像行（扫码后出现）
- "台板"Switch 开关 + "创建新牌局"主按钮
- 固定小组列表（组名 + 进行中状态 + 头像）
- 固定小组标题 + "管理"按钮（右上小字）
- 底部 3-tab Tab Bar
```

**Prompt 2: 创建计分页（对局 tab）**
```
基于 DESIGN.md 生成计分页-对局tab，包含：
- 顶部显示小组名（或"临时小组"）
- 倍率 Switch + 倍率输入框
- 总分排行列表（头像 + 分数，从高到低，冠军头像上有 👑 徽章）
- 对局历史大卡片：顶部头像行 + 局数列表（局数/时间/分数字段对齐）
- "截图分享"按钮
- 底部计分/对局 tab 切换
```

**Prompt 3: 创建计分页（计分 tab）**
```
基于 DESIGN.md 生成计分页-计分tab，包含：
- 计分表格：名称列(头像) | 胜负列(赢/输圆形单选) | 得分列(输入框+重置按钮)
- 确认按钮 → 弹窗"第X局分数已统计" → 跳到对局tab
- 底部：收盘按钮(金色) + "我们是固定搭子"按钮(仅临时小组显示)
- 收盘弹窗：输入倍率 → 展示总分 → "截图分享"按钮
```

**Prompt 4: 创建固定小组管理页**
```
基于 DESIGN.md 生成固定小组管理页，包含：
- 返回导航 + 标题"固定小组管理"
- 小组列表（每组：组名 + 成员头像 + 修改名称按钮）
- 点击修改 → 弹窗输入新名称
- 批量删除 + 一键清空按钮
```

**Prompt 5: 创建个人战绩页**
```
基于 DESIGN.md 生成个人战绩页，包含：
- 统计概览卡片（总局数、总胜场、胜率、最高分）
- 历史牌局列表（组名 + 头像 + 总分 + 日期）
- 返回按钮
```

### Iteration Guide

1. 始终用 CSS 变量，不要硬编码色值
2. 新增组件前检查是否已有可复用样式
3. 所有交互态（hover/active/disabled）必须定义
4. 模态框统一使用 overlay + content 结构，动画 0.2s
5. 输入框聚焦态必须有 ring shadow
6. 分数展示使用等宽字体 `font-family: 'JetBrains Mono', monospace`
7. 头像使用首字缩写或 emoji，fallback 到默认图标
8. Tab Bar 固定底部，content 区域 `padding-bottom: calc(64px + env(safe-area-inset-bottom))`
9. 主题切换通过 `document.documentElement.setAttribute('data-theme', theme)` 实现
10. 所有弹窗操作前先检查当前页面状态，避免重复弹窗
