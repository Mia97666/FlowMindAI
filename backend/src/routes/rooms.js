const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const { db } = require('../db');
const { authMiddleware } = require('../middleware/auth');

// 大部分牌局接口需要认证，status 接口允许匿名（放在 authMiddleware 之前）
// ==================== GET /rooms/:roomId/status ====================
// 轮询牌局状态（扫码加入时使用，不需要认证）
router.get('/:roomId/status', (req, res) => {
  try {
    const { roomId } = req.params;

    const room = db.prepare('SELECT status FROM rooms WHERE id = ?').get(roomId);
    if (!room) {
      return res.status(404).json({ code: 2001, message: '牌局不存在' });
    }

    const players = db.prepare(
      'SELECT user_id, avatar_url, total_score FROM room_players WHERE room_id = ?'
    ).all(roomId);

    res.json({
      status: room.status === 'active' ? (players.length >= 2 ? 'playing' : 'waiting') : 'closed',
      players: players.map(p => ({
        userId: p.user_id,
        avatarUrl: p.avatar_url,
        totalScore: p.total_score,
      })),
      playerCount: players.length,
    });
  } catch (err) {
    console.error('[rooms/status]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// 以下接口需要认证
router.use(authMiddleware);

// 确保用户存在（自动创建）
function ensureUser(userId) {
  const existing = db.prepare('SELECT open_id FROM users WHERE open_id = ?').get(userId);
  if (!existing) {
    db.prepare('INSERT INTO users (open_id, nick_name, avatar_url) VALUES (?, ?, ?)').run(
      userId, '麻友' + userId.slice(-4), ''
    );
  }
  return userId;
}

// ==================== POST /rooms/create ====================
// 创建新牌局
router.post('/create', (req, res) => {
  try {
    const { groupId } = req.body || {};
    const roomId = uuidv4();
    const openId = req.openId;

    let roomName = '麻将计分';
    let players = [];

    if (groupId) {
      // 使用固定小组
      const group = db.prepare('SELECT * FROM groups_table WHERE id = ? AND created_by = ?').get(groupId, openId);
      if (!group) {
        return res.status(404).json({ code: 3001, message: '小组不存在' });
      }

      // 检查是否有进行中的牌局
      const activeRoom = db.prepare(
        'SELECT id FROM rooms WHERE group_id = ? AND status = ? AND created_by = ?'
      ).get(groupId, 'active', openId);

      if (activeRoom) {
        return res.json({
          roomId: activeRoom.id,
          isExisting: true,
          message: '该小组已有进行中的牌局',
        });
      }

      roomName = group.name;
      const members = db.prepare('SELECT * FROM group_members WHERE group_id = ?').all(groupId);
      players = members;
    }

    // 创建牌局
    db.prepare(`
      INSERT INTO rooms (id, name, group_id, status, multiplier, multiplier_enabled, table_fee_enabled, created_by)
      VALUES (?, ?, ?, 'active', 1.0, 0, 0, ?)
    `).run(roomId, roomName, groupId || null, openId);

    // 创建者自动加入
    const user = db.prepare('SELECT * FROM users WHERE open_id = ?').get(openId);
    db.prepare('INSERT OR IGNORE INTO room_players (room_id, user_id, avatar_url) VALUES (?, ?, ?)').run(
      roomId, openId, user?.avatar_url || ''
    );

    // 如果是固定小组，添加其他成员
    if (players.length > 0) {
      const insertPlayer = db.prepare(
        'INSERT OR IGNORE INTO room_players (room_id, user_id, avatar_url) VALUES (?, ?, ?)'
      );
      for (const p of players) {
        if (p.user_id !== openId) {
          insertPlayer.run(roomId, p.user_id, p.avatar_url || '');
        }
      }
    }

    // 生成场景值用于小程序码
    const scene = `roomId=${roomId}`;

    res.status(201).json({
      roomId,
      scene,
      qrCodeUrl: '', // 生产环境调用微信 API 生成
      name: roomName,
    });
  } catch (err) {
    console.error('[rooms/create]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== GET /rooms ====================
// 获取牌局列表
router.get('/', (req, res) => {
  try {
    const openId = req.openId;
    const { status, page = 1, size = 20 } = req.query;
    const offset = (parseInt(page) - 1) * parseInt(size);

    let whereClause = 'WHERE rp.user_id = ?';
    const params = [openId];

    if (status && (status === 'active' || status === 'closed')) {
      whereClause += ' AND r.status = ?';
      params.push(status);
    }

    const countSql = `
      SELECT COUNT(DISTINCT r.id) as total
      FROM rooms r
      INNER JOIN room_players rp ON r.id = rp.room_id
      ${whereClause}
    `;
    const { total } = db.prepare(countSql).get(...params);

    const sql = `
      SELECT DISTINCT r.*
      FROM rooms r
      INNER JOIN room_players rp ON r.id = rp.room_id
      ${whereClause}
      ORDER BY r.created_at DESC
      LIMIT ? OFFSET ?
    `;
    const rooms = db.prepare(sql).all(...params, parseInt(size), offset);

    // 为每个牌局填充玩家信息
    const items = rooms.map(room => {
      const players = db.prepare(
        'SELECT user_id, avatar_url, total_score FROM room_players WHERE room_id = ?'
      ).all(room.id);
      const rounds = db.prepare(
        'SELECT id, round_number, time FROM rounds WHERE room_id = ? ORDER BY round_number'
      ).all(room.id);

      return {
        id: room.id,
        name: room.name,
        groupId: room.group_id,
        status: room.status,
        multiplier: room.multiplier,
        multiplierEnabled: !!room.multiplier_enabled,
        players: players.map(p => ({
          userId: p.user_id,
          avatarUrl: p.avatar_url,
          totalScore: p.total_score,
        })),
        roundCount: rounds.length,
        createdAt: room.created_at,
        closedAt: room.closed_at,
      };
    });

    res.json({ items, total, page: parseInt(page), size: parseInt(size) });
  } catch (err) {
    console.error('[rooms/list]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== GET /rooms/:roomId ====================
// 获取牌局详情
router.get('/:roomId', (req, res) => {
  try {
    const openId = req.openId;
    const { roomId } = req.params;

    const room = db.prepare('SELECT * FROM rooms WHERE id = ?').get(roomId);
    if (!room) {
      return res.status(404).json({ code: 2001, message: '牌局不存在' });
    }

    // 验证用户是否是该牌局的参与者
    const isPlayer = db.prepare(
      'SELECT 1 FROM room_players WHERE room_id = ? AND user_id = ?'
    ).get(roomId, openId);
    if (!isPlayer) {
      return res.status(403).json({ code: 2003, message: '非牌局参与者' });
    }

    const players = db.prepare(
      'SELECT user_id, avatar_url, total_score FROM room_players WHERE room_id = ?'
    ).all(roomId);

    const roundRows = db.prepare(
      'SELECT id, round_number, time FROM rounds WHERE room_id = ? ORDER BY round_number'
    ).all(roomId);

    const rounds = roundRows.map(r => {
      const scores = db.prepare(
        'SELECT user_id, score FROM round_scores WHERE round_id = ?'
      ).all(r.id);
      const scoreMap = {};
      scores.forEach(s => { scoreMap[s.user_id] = s.score; });
      return {
        roundNumber: r.round_number,
        time: r.time,
        scores: scoreMap,
      };
    });

    res.json({
      id: room.id,
      name: room.name,
      groupId: room.group_id,
      status: room.status,
      multiplier: room.multiplier,
      multiplierEnabled: !!room.multiplier_enabled,
      tableFeeEnabled: !!room.table_fee_enabled,
      createdBy: room.created_by,
      players: players.map(p => ({
        userId: p.user_id,
        avatarUrl: p.avatar_url,
        totalScore: p.total_score,
      })),
      rounds,
      createdAt: room.created_at,
      closedAt: room.closed_at,
    });
  } catch (err) {
    console.error('[rooms/detail]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== POST /rooms/:roomId/join ====================
// 扫码加入牌局
router.post('/:roomId/join', (req, res) => {
  try {
    const openId = req.openId;
    const { roomId } = req.params;

    const room = db.prepare('SELECT * FROM rooms WHERE id = ?').get(roomId);
    if (!room) {
      return res.status(404).json({ code: 2001, message: '牌局不存在' });
    }

    if (room.status === 'closed') {
      return res.status(400).json({ code: 2002, message: '牌局已收盘，无法加入' });
    }

    const existing = db.prepare(
      'SELECT 1 FROM room_players WHERE room_id = ? AND user_id = ?'
    ).get(roomId, openId);

    let isNewMember = false;
    if (!existing) {
      const user = db.prepare('SELECT * FROM users WHERE open_id = ?').get(openId);
      db.prepare('INSERT OR IGNORE INTO room_players (room_id, user_id, avatar_url) VALUES (?, ?, ?)').run(
        roomId, openId, user?.avatar_url || ''
      );
      isNewMember = true;
    }

    // 检查是否是固定小组的新成员
    let isNewGroup = false;
    if (room.group_id) {
      const inGroup = db.prepare(
        'SELECT 1 FROM group_members WHERE group_id = ? AND user_id = ?'
      ).get(room.group_id, openId);
      if (!inGroup) {
        db.prepare('INSERT OR IGNORE INTO group_members (group_id, user_id, avatar_url) VALUES (?, ?, ?)').run(
          room.group_id, openId, ''
        );
        isNewGroup = true;
      }

      // ★ 自动建立好友关系：加入同一组的所有成员互为好友
      const groupInfo = db.prepare('SELECT name FROM groups_table WHERE id = ?').get(room.group_id);
      const groupMembers = db.prepare(
        'SELECT user_id FROM group_members WHERE group_id = ?'
      ).all(room.group_id);
      const insertFriend = db.prepare(
        'INSERT OR IGNORE INTO friends (user_id, friend_id, group_id, group_name) VALUES (?, ?, ?, ?)'
      );
      for (const m of groupMembers) {
        insertFriend.run(openId, m.user_id, room.group_id, groupInfo?.name || '');
        insertFriend.run(m.user_id, openId, room.group_id, groupInfo?.name || '');
      }
    }

    res.json({
      roomId,
      isNewMember,
      isNewGroup,
    });
  } catch (err) {
    console.error('[rooms/join]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== POST /rooms/:roomId/round ====================
// 提交一局分数
router.post('/:roomId/round', (req, res) => {
  try {
    const openId = req.openId;
    const { roomId } = req.params;
    const { scores } = req.body; // { "userId1": 8, "userId2": -4, ... }

    if (!scores || typeof scores !== 'object') {
      return res.status(400).json({ code: 1000, message: '缺少必要参数 scores' });
    }

    const room = db.prepare('SELECT * FROM rooms WHERE id = ?').get(roomId);
    if (!room) {
      return res.status(404).json({ code: 2001, message: '牌局不存在' });
    }
    if (room.status === 'closed') {
      return res.status(400).json({ code: 2002, message: '牌局已收盘，不可修改' });
    }

    // 验证分数总和为 0（或接近 0）
    const total = Object.values(scores).reduce((sum, s) => sum + s, 0);
    if (Math.abs(total) > 0.01) {
      return res.status(400).json({ code: 1000, message: '分数总和不等于 0' });
    }

    // 获取当前局数
    const maxRound = db.prepare(
      'SELECT MAX(round_number) as max_round FROM rounds WHERE room_id = ?'
    ).get(roomId);
    const roundNumber = (maxRound?.max_round || 0) + 1;

    // 开事务：写入局记录 + 更新玩家总分
    const insertRound = db.prepare(
      'INSERT INTO rounds (room_id, round_number) VALUES (?, ?)'
    );
    const insertScore = db.prepare(
      'INSERT INTO round_scores (round_id, user_id, score) VALUES (?, ?, ?)'
    );
    const updatePlayer = db.prepare(
      'UPDATE room_players SET total_score = total_score + ? WHERE room_id = ? AND user_id = ?'
    );

    const txn = db.transaction(() => {
      const result = insertRound.run(roomId, roundNumber);
      const roundId = result.lastInsertRowid;

      for (const [userId, score] of Object.entries(scores)) {
        ensureUser(userId); // 自动创建用户
        insertScore.run(roundId, userId, score);
        updatePlayer.run(score, roomId, userId);
      }
    });

    txn();

    // 返回更新后的牌局状态
    const players = db.prepare(
      'SELECT user_id, avatar_url, total_score FROM room_players WHERE room_id = ? ORDER BY total_score DESC'
    ).all(roomId);

    const roundScores = db.prepare(
      'SELECT user_id, score FROM round_scores WHERE round_id = (SELECT id FROM rounds WHERE room_id = ? AND round_number = ?)'
    ).all(roomId, roundNumber);
    const scoreMap = {};
    roundScores.forEach(s => { scoreMap[s.user_id] = s.score; });

    res.status(201).json({
      round: {
        roundNumber,
        time: new Date().toISOString(),
        scores: scoreMap,
      },
      players: players.map(p => ({
        userId: p.user_id,
        avatarUrl: p.avatar_url,
        totalScore: p.total_score,
      })),
    });
  } catch (err) {
    console.error('[rooms/round]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== PUT /rooms/:roomId/close ====================
// 收盘
router.put('/:roomId/close', (req, res) => {
  try {
    const openId = req.openId;
    const { roomId } = req.params;

    const room = db.prepare('SELECT * FROM rooms WHERE id = ?').get(roomId);
    if (!room) {
      return res.status(404).json({ code: 2001, message: '牌局不存在' });
    }
    if (room.status === 'closed') {
      return res.status(400).json({ code: 2002, message: '牌局已经收盘' });
    }

    db.prepare('UPDATE rooms SET status = ?, closed_at = datetime(?) WHERE id = ?').run(
      'closed', new Date().toISOString(), roomId
    );

    const players = db.prepare(
      'SELECT user_id, avatar_url, total_score FROM room_players WHERE room_id = ? ORDER BY total_score DESC'
    ).all(roomId);

    res.json({
      room: {
        ...room,
        status: 'closed',
        closedAt: new Date().toISOString(),
        players: players.map(p => ({
          userId: p.user_id,
          avatarUrl: p.avatar_url,
          totalScore: p.total_score,
        })),
      },
    });
  } catch (err) {
    console.error('[rooms/close]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== PATCH /rooms/:roomId/multiplier ====================
// 更新倍率
router.patch('/:roomId/multiplier', (req, res) => {
  try {
    const { roomId } = req.params;
    const { multiplier, enabled } = req.body;

    const room = db.prepare('SELECT * FROM rooms WHERE id = ?').get(roomId);
    if (!room) {
      return res.status(404).json({ code: 2001, message: '牌局不存在' });
    }

    if (multiplier !== undefined) {
      db.prepare('UPDATE rooms SET multiplier = ? WHERE id = ?').run(multiplier, roomId);
    }
    if (enabled !== undefined) {
      db.prepare('UPDATE rooms SET multiplier_enabled = ? WHERE id = ?').run(enabled ? 1 : 0, roomId);
    }

    const updated = db.prepare('SELECT * FROM rooms WHERE id = ?').get(roomId);
    res.json({
      multiplier: updated.multiplier,
      multiplierEnabled: !!updated.multiplier_enabled,
    });
  } catch (err) {
    console.error('[rooms/multiplier]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== PATCH /rooms/:roomId/table-fee ====================
// 台板费开关
router.patch('/:roomId/table-fee', (req, res) => {
  try {
    const { roomId } = req.params;
    const { enabled } = req.body;

    db.prepare('UPDATE rooms SET table_fee_enabled = ? WHERE id = ?').run(enabled ? 1 : 0, roomId);
    const updated = db.prepare('SELECT * FROM rooms WHERE id = ?').get(roomId);

    res.json({ tableFeeEnabled: !!updated.table_fee_enabled });
  } catch (err) {
    console.error('[rooms/table-fee]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== POST /rooms/:roomId/leave ====================
// 离开牌局
router.post('/:roomId/leave', (req, res) => {
  try {
    const openId = req.openId;
    const { roomId } = req.params;

    const result = db.prepare(
      'DELETE FROM room_players WHERE room_id = ? AND user_id = ?'
    ).run(roomId, openId);

    res.json({ left: result.changes > 0 });
  } catch (err) {
    console.error('[rooms/leave]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

module.exports = router;
