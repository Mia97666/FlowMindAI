const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'mahjong-scorer-jwt-secret-dev';
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '2h';
const REFRESH_TOKEN_EXPIRES_IN = process.env.REFRESH_TOKEN_EXPIRES_IN || '30d';

// 生成 access token
function generateAccessToken(openId) {
  return jwt.sign({ openId, type: 'access' }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
}

// 生成 refresh token
function generateRefreshToken(openId) {
  return jwt.sign({ openId, type: 'refresh' }, JWT_SECRET, { expiresIn: REFRESH_TOKEN_EXPIRES_IN });
}

// 验证 token
function verifyToken(token) {
  try {
    return jwt.verify(token, JWT_SECRET);
  } catch (err) {
    return null;
  }
}

// 认证中间件
function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ code: 1002, message: 'Token 无效' });
  }

  const token = authHeader.slice(7);
  const payload = verifyToken(token);

  if (!payload || payload.type !== 'access') {
    return res.status(401).json({ code: 1001, message: 'Token 已过期，请重新登录' });
  }

  req.openId = payload.openId;
  next();
}

module.exports = {
  generateAccessToken,
  generateRefreshToken,
  verifyToken,
  authMiddleware,
  JWT_SECRET,
};
