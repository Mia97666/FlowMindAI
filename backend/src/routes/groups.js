const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const { db } = require('../db');
const { authMiddleware } = require('../middleware/auth');

router.use(authMiddleware);

// ==================== POST /groups ====================
// 创建固定小组（从已收盘的牌局保存）
router.post('/', (req, res) => {
  try {
    const openId = req.openId;
    const { name, roomId: fromRoomId } = req.body;

    if (!name || !name.trim()) {
      return res.status(400).json({ code: 1000, message: '小组名称不能为空' });
    }

    const groupId = uuidv4();

    db.prepare('INSERT INTO groups_table (id, name, created_by) VALUES (?, ?, ?)').run(
      groupId, name.trim(), openId
    );

    // 从已有牌局导入成员
    if (fromRoomId) {
      const room = db.prepare('SELECT * FROM rooms WHERE id = ?').get(fromRoomId);
      if (room) {
        const players = db.prepare(
          'SELECT user_id, avatar_url FROM room_players WHERE room_id = ?'
        ).all(fromRoomId);

        const insertMember = db.prepare(
          'INSERT OR IGNORE INTO group_members (group_id, user_id, avatar_url) VALUES (?, ?, ?)'
        );
        for (const p of players) {
          insertMember.run(groupId, p.user_id, p.avatar_url || '');
        }
      }
    }

    // 创建者自动加入
    db.prepare(
      'INSERT OR IGNORE INTO group_members (group_id, user_id, avatar_url) VALUES (?, ?, ?)'
    ).run(groupId, openId, '');

    // ★ 自动建立好友关系：同一组内的成员互为好友
    const members = db.prepare(
      'SELECT user_id, avatar_url FROM group_members WHERE group_id = ?'
    ).all(groupId);
    const insertFriend = db.prepare(
      'INSERT OR IGNORE INTO friends (user_id, friend_id, group_id, group_name) VALUES (?, ?, ?, ?)'
    );
    for (const m of members) {
      // 双向好友
      insertFriend.run(openId, m.user_id, groupId, name.trim());
      insertFriend.run(m.user_id, openId, groupId, name.trim());
    }
    // 自己也加入好友表（方便排行查询）
    insertFriend.run(openId, openId, groupId, name.trim());

    res.status(201).json({
      id: groupId,
      name: name.trim(),
      members: members.map(m => ({
        userId: m.user_id,
        avatarUrl: m.avatar_url,
      })),
      memberCount: members.length,
      createdAt: new Date().toISOString(),
    });
  } catch (err) {
    console.error('[groups/create]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== GET /groups ====================
// 获取所有固定小组
router.get('/', (req, res) => {
  try {
    const openId = req.openId;

    const groups = db.prepare(
      'SELECT * FROM groups_table WHERE created_by = ? ORDER BY created_at DESC'
    ).all(openId);

    const items = groups.map(g => {
      const members = db.prepare(
        'SELECT user_id, avatar_url FROM group_members WHERE group_id = ?'
      ).all(g.id);

      // 检查是否有进行中的牌局
      const activeRoom = db.prepare(
        'SELECT id FROM rooms WHERE group_id = ? AND status = ?'
      ).get(g.id, 'active');

      return {
        id: g.id,
        name: g.name,
        members: members.map(m => ({
          userId: m.user_id,
          avatarUrl: m.avatar_url,
        })),
        memberCount: members.length,
        hasActiveGame: !!activeRoom,
        activeRoomId: activeRoom?.id || null,
        createdAt: g.created_at,
      };
    });

    res.json({ items, total: items.length });
  } catch (err) {
    console.error('[groups/list]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== PATCH /groups/:id ====================
// 更新小组名称
router.patch('/:id', (req, res) => {
  try {
    const openId = req.openId;
    const { id } = req.params;
    const { name } = req.body;

    const group = db.prepare('SELECT * FROM groups_table WHERE id = ? AND created_by = ?').get(id, openId);
    if (!group) {
      return res.status(404).json({ code: 3001, message: '小组不存在' });
    }

    if (name && name.trim()) {
      db.prepare('UPDATE groups_table SET name = ? WHERE id = ?').run(name.trim(), id);
    }

    const updated = db.prepare('SELECT * FROM groups_table WHERE id = ?').get(id);
    res.json({ id: updated.id, name: updated.name });
  } catch (err) {
    console.error('[groups/update]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== DELETE /groups/batch ====================
// 批量删除小组
router.delete('/batch', (req, res) => {
  try {
    const openId = req.openId;
    const { ids } = req.body;

    if (!Array.isArray(ids) || ids.length === 0) {
      return res.status(400).json({ code: 1000, message: '请指定要删除的小组' });
    }

    const placeholders = ids.map(() => '?').join(',');
    const result = db.prepare(
      `DELETE FROM groups_table WHERE id IN (${placeholders}) AND created_by = ?`
    ).run(...ids, openId);

    res.json({ deletedCount: result.changes });
  } catch (err) {
    console.error('[groups/batchDelete]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== DELETE /groups/all ====================
// 一键清空
router.delete('/all', (req, res) => {
  try {
    const openId = req.openId;
    const result = db.prepare('DELETE FROM groups_table WHERE created_by = ?').run(openId);
    res.json({ deletedCount: result.changes });
  } catch (err) {
    console.error('[groups/all]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

module.exports = router;
