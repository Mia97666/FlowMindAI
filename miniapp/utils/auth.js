const { request } = require('./request');

// 是否使用 Mock 数据（与 request.js 的 USE_MOCK 保持一致）
const USE_MOCK = false;

async function login() {
  try {
    let code;

    if (USE_MOCK) {
      code = 'dev_mock_code';
    } else {
      const loginResult = await new Promise((resolve, reject) => {
        wx.login({ success: resolve, fail: reject });
      });
      code = loginResult.code;
    }

    const result = await request({
      url: '/auth/login',
      method: 'POST',
      data: { code },
    });

    wx.setStorageSync('token', result.accessToken);
    wx.setStorageSync('refreshToken', result.refreshToken);

    return result;
  } catch (err) {
    console.error('Login failed:', err);
    throw err;
  }
}

async function refreshToken() {
  const refresh = wx.getStorageSync('refreshToken');
  if (!refresh) throw new Error('No refresh token');

  const result = await request({
    url: '/auth/refresh',
    method: 'POST',
    data: { refreshToken: refresh },
  });

  wx.setStorageSync('token', result.accessToken);
  wx.setStorageSync('refreshToken', result.refreshToken);

  return result;
}

function isLoggedIn() {
  return !!wx.getStorageSync('token');
}

module.exports = { login, refreshToken, isLoggedIn };
