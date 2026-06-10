# 麻将计分 — 开发阶段 1 完成

## ✅ 已完成

### 后端服务（backend/）
- **数据库**：SQLite 6 表设计（users, rooms, room_players, rounds, round_scores, groups, group_members）
- **API 接口**：17 个 RESTful 接口（认证、牌局 CRUD、小组管理），全部已测试通过
- **认证**：JWT Bearer Token（access + refresh token 双重机制）
- **技术栈**：Express + better-sqlite3 + jsonwebtoken
- **运行方式**：`cd backend && npm start` → http://localhost:3000

### 小程序（miniapp/）
- **12 个文件**：app.js/json/wxss + project.config.json + sitemap.json
- **7 个页面**：index / scoring / games / profile / groups / stats / scoreboard
- **10 个组件**：player-avatar, qr-card, score-input-row, win-lose-radio, game-card, round-history, multiplier-control, group-list-item, custom-tab-bar, theme-toggle
- **工具模块**：request.js, auth.js, theme.js, constants.js
- **服务模块**：room.js, group.js
- **双主题系统**：CSS 变量驱动，暗色/亮色 + 跟随系统

## 📋 待完成

1. 提供 AppID 替换 `miniapp/project.config.json` 中的 `YOUR_APPID_HERE`
2. 在微信开发者工具中打开 `miniapp/` 目录
3. 安装 TDesign：`cd miniapp && npm install`，然后在开发工具中「工具 → 构建 npm」
4. 部署后端到云服务器（替换 `.env` 中的 JWT_SECRET）
5. 配置 HTTPS 证书（微信小程序强制要求）
6. 配置微信公众平台服务器域名白名单
