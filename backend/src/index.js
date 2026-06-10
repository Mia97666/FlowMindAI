const express = require('express');
const cors = require('cors');
const path = require('path');

// 加载环境变量（开发时使用 .env 文件，生产环境用系统环境变量）
try {
  const envPath = path.join(__dirname, '..', '.env');
  if (require('fs').existsSync(envPath)) {
    const envContent = require('fs').readFileSync(envPath, 'utf-8');
    envContent.split('\n').forEach(line => {
      const trimmed = line.trim();
      if (trimmed && !trimmed.startsWith('#')) {
        const [key, ...vals] = trimmed.split('=');
        const value = vals.join('=').trim();
        if (key && value && !process.env[key]) {
          process.env[key] = value;
        }
      }
    });
  }
} catch (e) {
  // .env 文件不存在，使用系统环境变量
}

const { initDB } = require('./db');
const authRoutes = require('./routes/auth');
const roomRoutes = require('./routes/rooms');
const groupRoutes = require('./routes/groups');
const rankingRoutes = require('./routes/ranking');

// 初始化数据库
initDB();

const app = express();
const PORT = process.env.PORT || 3000;

// 中间件
app.use(cors());
app.use(express.json());

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', time: new Date().toISOString() });
});

// API 路由
app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/rooms', roomRoutes);
app.use('/api/v1/groups', groupRoutes);
app.use('/api/v1/ranking', rankingRoutes);

// 全局错误处理
app.use((err, req, res, next) => {
  console.error('[ERROR]', err);
  res.status(500).json({
    code: 4000,
    message: process.env.NODE_ENV === 'production'
      ? '服务器内部错误'
      : err.message,
  });
});

// 404 处理
app.use((req, res) => {
  res.status(404).json({ code: 404, message: '接口不存在' });
});

app.listen(PORT, () => {
  console.log(`[Server] 麻将计分 API 服务已启动: http://localhost:${PORT}`);
  console.log(`[Server] 环境: ${process.env.NODE_ENV || 'development'}`);
  console.log('[Server] 可用接口:');
  console.log('  POST   /api/v1/auth/login');
  console.log('  POST   /api/v1/auth/refresh');
  console.log('  POST   /api/v1/rooms/create');
  console.log('  GET    /api/v1/rooms');
  console.log('  GET    /api/v1/rooms/:roomId');
  console.log('  GET    /api/v1/rooms/:roomId/status');
  console.log('  POST   /api/v1/rooms/:roomId/join');
  console.log('  POST   /api/v1/rooms/:roomId/round');
  console.log('  PUT    /api/v1/rooms/:roomId/close');
  console.log('  PATCH  /api/v1/rooms/:roomId/multiplier');
  console.log('  PATCH  /api/v1/rooms/:roomId/table-fee');
  console.log('  POST   /api/v1/rooms/:roomId/leave');
  console.log('  POST   /api/v1/groups');
  console.log('  GET    /api/v1/groups');
  console.log('  PATCH  /api/v1/groups/:id');
  console.log('  DELETE /api/v1/groups/batch');
  console.log('  DELETE /api/v1/groups/all');
  console.log('  GET    /api/v1/ranking');
});

module.exports = app;
