const express = require('express');
const router = express.Router();
const https = require('https');
const { db } = require('../db');
const { generateAccessToken, generateRefreshToken, verifyToken } = require('../middleware/auth');

const WECHAT_APPID = process.env.WECHAT_APPID || '';
const WECHAT_SECRET = process.env.WECHAT_SECRET || '';

// ==================== POST /auth/login ====================
// 微信登录：用 code 换取 token
router.post('/login', async (req, res) => {
  try {
    const { code } = req.body;
    if (!code) {
      return res.status(400).json({ code: 1000, message: '缺少必要参数 code' });
    }

    let openId;

    // 尝试调用微信 API 换取 openId
    if (WECHAT_APPID && WECHAT_SECRET) {
      try {
        const wxData = await new Promise((resolve, reject) => {
          const url = `https://api.weixin.qq.com/sns/jscode2session?appid=${WECHAT_APPID}&secret=${WECHAT_SECRET}&js_code=${code}&grant_type=authorization_code`;
          https.get(url, (resp) => {
            let data = '';
            resp.on('data', chunk => data += chunk);
            resp.on('end', () => {
              try { resolve(JSON.parse(data)); } catch (e) { reject(e); }
            });
          }).on('error', reject);
        });

        if (!wxData.errcode && wxData.openid) {
          openId = wxData.openid;
        } else {
          console.warn('[auth] WeChat API error, fallback to dev mode:', wxData.errmsg);
        }
      } catch (wxErr) {
        console.warn('[auth] WeChat API call failed, fallback to dev mode:', wxErr.message);
      }
    }

    // 降级：开发模式
    if (!openId) {
      openId = code.startsWith('dev_') ? code : `dev_${Date.now()}`;
    }

    // 查找或创建用户
    let user = db.prepare('SELECT * FROM users WHERE open_id = ?').get(openId);
    if (!user) {
      db.prepare('INSERT INTO users (open_id, nick_name, avatar_url) VALUES (?, ?, ?)').run(
        openId, '麻将爱好者', ''
      );
      user = db.prepare('SELECT * FROM users WHERE open_id = ?').get(openId);
    }

    const accessToken = generateAccessToken(openId);
    const refreshToken = generateRefreshToken(openId);

    res.json({
      accessToken,
      refreshToken,
      user: {
        openId: user.open_id,
        nickName: user.nick_name,
        avatarUrl: user.avatar_url,
      },
    });
  } catch (err) {
    console.error('[auth/login]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

// ==================== POST /auth/refresh ====================
// 刷新 token
router.post('/refresh', (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(400).json({ code: 1000, message: '缺少必要参数 refreshToken' });
    }

    const payload = verifyToken(refreshToken);
    if (!payload || payload.type !== 'refresh') {
      return res.status(401).json({ code: 1001, message: 'Refresh token 无效或已过期' });
    }

    const newAccessToken = generateAccessToken(payload.openId);
    const newRefreshToken = generateRefreshToken(payload.openId);

    res.json({ accessToken: newAccessToken, refreshToken: newRefreshToken });
  } catch (err) {
    console.error('[auth/refresh]', err);
    res.status(500).json({ code: 4000, message: '服务器内部错误' });
  }
});

module.exports = router;
