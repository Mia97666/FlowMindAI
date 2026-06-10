// 本地 Mock 服务器 - 用于开发时前后端联调
const express = require('express');
const cors = require('cors');
const app = express();
const PORT = 3456;

app.use(cors());
app.use(express.json());

// Mock 数据
const users = {};
const rooms = {};
const groups = [];

// ===== 健康检查 =====
app.get('/api/health', (req, res) => res.json({ status: 'ok', mock: true }));

// ===== 登录 =====
app.post('/api/v1/auth/login', (req, res) => {
  const { code } = req.body;
  const openId = code || 'dev_user_mock';
  users[openId] = users[openId] || { openId, nickName: '麻友', avatarUrl: '' };
  res.json({
    accessToken: 'mock_token_' + openId,
    refreshToken: 'mock_refresh_' + openId,
    user: users[openId],
  });
});

// ===== 创建牌局 =====
app.post('/api/v1/rooms/create', (req, res) => {
  const roomId = 'room_' + Date.now();
  rooms[roomId] = {
    id: roomId, name: '麻将计分', groupId: null, status: 'active',
    multiplier: 1.0, multiplierEnabled: false, tableFeeEnabled: false,
    players: [{ userId: 'dev_user_mock', avatarUrl: '', totalScore: 0 }],
    rounds: [], createdAt: new Date().toISOString(), closedAt: null,
  };
  res.json({ roomId, scene: 'roomId=' + roomId, name: '麻将计分' });
});

// ===== 牌局状态 =====
app.get('/api/v1/rooms/:id/status', (req, res) => {
  const room = rooms[req.params.id];
  if (!room) return res.status(404).json({ code: 2001, message: '牌局不存在' });
  res.json({ status: room.players.length >= 2 ? 'playing' : 'waiting', players: room.players, playerCount: room.players.length });
});

// ===== 加入牌局 =====
app.post('/api/v1/rooms/:id/join', (req, res) => {
  const room = rooms[req.params.id];
  if (!room) return res.status(404).json({ code: 2001, message: '牌局不存在' });
  // 模拟多人加入
  const mockPlayers = ['dev_user_b', 'dev_user_c', 'dev_user_d'];
  const existing = room.players.map(p => p.userId);
  mockPlayers.forEach(id => {
    if (!existing.includes(id)) {
      room.players.push({ userId: id, avatarUrl: '', totalScore: 0 });
    }
  });
  res.json({ roomId: req.params.id, isNewGroup: false });
});

// ===== 牌局详情 =====
app.get('/api/v1/rooms/:id', (req, res) => {
  const room = rooms[req.params.id];
  if (!room) return res.status(404).json({ code: 2001, message: '牌局不存在' });
  res.json(room);
});

// ===== 提交分数 =====
app.post('/api/v1/rooms/:id/round', (req, res) => {
  const room = rooms[req.params.id];
  if (!room) return res.status(404).json({ code: 2001, message: '牌局不存在' });
  const { scores } = req.body;
  const roundNumber = room.rounds.length + 1;
  const round = { roundNumber, time: new Date().toISOString(), scores };
  room.rounds.push(round);
  // 更新总分
  Object.entries(scores).forEach(([userId, score]) => {
    const p = room.players.find(p => p.userId === userId);
    if (p) p.totalScore = (p.totalScore || 0) + score;
    else room.players.push({ userId, avatarUrl: '', totalScore: score });
  });
  res.json({ round, players: room.players });
});

// ===== 收盘 =====
app.put('/api/v1/rooms/:id/close', (req, res) => {
  const room = rooms[req.params.id];
  if (!room) return res.status(404).json({ code: 2001, message: '牌局不存在' });
  room.status = 'closed';
  room.closedAt = new Date().toISOString();
  res.json({ room });
});

// ===== 牌局列表 =====
app.get('/api/v1/rooms', (req, res) => {
  const items = Object.values(rooms).map(r => ({
    id: r.id, name: r.name, groupId: r.groupId, status: r.status,
    players: r.players, roundCount: r.rounds.length,
    createdAt: r.createdAt, closedAt: r.closedAt,
  }));
  res.json({ items, total: items.length, page: 1, size: 20 });
});

// ===== 小组管理 =====
app.get('/api/v1/groups', (req, res) => {
  res.json({ items: groups, total: groups.length });
});

app.post('/api/v1/groups', (req, res) => {
  const g = { id: 'g_' + Date.now(), name: req.body.name, members: [{ userId: 'dev_user_mock', avatarUrl: '' }], memberCount: 1, hasActiveGame: false, activeRoomId: null, createdAt: new Date().toISOString() };
  groups.push(g);
  res.json(g);
});

app.delete('/api/v1/groups/batch', (req, res) => {
  res.json({ deletedCount: 1 });
});

app.listen(PORT, () => {
  console.log(`[Mock] 麻将计分 Mock API: http://localhost:${PORT}`);
  console.log('[Mock] 可用接口: auth/login, rooms/create, rooms/:id, rooms/:id/round, rooms/:id/close, groups');
});
