const express = require('express');
const router = express.Router();
const { db } = require('../db');
const { authMiddleware } = require('../middleware/auth');

router.use(authMiddleware);

// ==================== GET /ranking ====================
// 麻神排行：支持 type=today / type=total
router.get('/', (req, res) => {
  try {
    const openId = req.openId;
    const { type = 'total' } = req.query;

    // 1. 获取好友列表（含自己）
    const friends = db.prepare(
      'SELECT friend_id, group_name FROM friends WHERE user_id = ?'
    ).all(openId);

    // 包含自己在内
    const userIds = [openId, ...friends.map(f => f.friend_id)];
    const groupNameMap = {};
    friends.forEach(f => { groupNameMap[f.friend_id] = f.group_name; });

    // 2.算每个用户的分数
    let scoreQuery;
    if (type === 'today') {
      // 当日分数：仅统计今天收盘的牌局
      scoreQuery = `
        SELECT rp.user_id, SUM(rp.total_score) as total
        FROM room_players rp
        INNER JOIN rooms r ON rp.room_id = r.id
        WHERE rp.user_id IN (${userIds.map(() => '?').join(',')})
          AND r.status = 'closed'
          AND date(r.closed_at) = date('now')
        GROUP BY rp.user_id
      `;
    } else {
      // 总计分数：所有收盘牌局
      scoreQuery = `
        SELECT rp.user_id, SUM(rp.total_score) as total
        FROM room_players rp
        INNER JOIN rooms r ON rp.room_id = r.id
        WHERE rp.user_id IN (${userIds.map(() => '?').join(',')})
          AND r.status = 'closed'
        GROUP BY rp.user_id
      `;
    }

    const scores = db.prepare(scoreQuery).all(...userIds);
    const scoreMap = {};
    scores.forEach(s => { scoreMap[s.user_id] = Number((s.total || 0).toFixed(2)); });

    // 3. 组装排行数据
    const items = userIds.map(uid => {
      const user = db.prepare('SELECT open_id, nick_name, avatar_url FROM users WHERE open_id = ?').get(uid);
      return {
        userId: uid,
        nickname: user?.nick_name || '麻友' + uid.slice(-4),
        avatarUrl: user?.avatar_url || '',
        score: scoreMap[uid] || 0,
        groupName: groupNameMap[uid] || '',
        isMe: uid === openId,
      };
    }).filter(item => item.score !== 0) // 只展示有分数的
      .sort((a, b) => b.score - a.score); // 按分数倒序

    // 添加排名
    items.forEach((item, idx) => { item.rank = idx + 1; });

    res.json({ items, type });
  } catch (err) {
    console.error('[ranking]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== DELETE /friend ====================
// 删除好友
router.post('/friend/delete', (req, res) => {
  try {
    const openId = req.openId;
    const { friendUserId } = req.body;
    if (!friendUserId) {
      return res.status(400).json({ code: 4000, message: '缺少 friendUserId' });
    }
    // 双向删除：同时删除好友表中 user_id <-> friend_id 的两条记录
    const r1 = db.prepare(
      'DELETE FROM friends WHERE user_id = ? AND friend_id = ?'
    ).run(openId, friendUserId);
    const r2 = db.prepare(
      'DELETE FROM friends WHERE user_id = ? AND friend_id = ?'
    ).run(friendUserId, openId);
    res.json({ code: 0, message: '删除成功', changes: r1.changes + r2.changes });
  } catch (err) {
    console.error('[ranking] DELETE /friend', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

module.exports = router;
