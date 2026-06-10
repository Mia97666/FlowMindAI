// API 地址
// 注意：域名 xagentic.top 尚未备案，暂时使用 IP 直连
// 域名备案完成后改为：PROD_BASE_URL = 'https://xagentic.top/api/v1'
const PROD_BASE_URL = 'http://150.158.119.197/api/v1';
const DEV_BASE_URL = 'http://150.158.119.197/api/v1';

// Mock 开关：设为 false 连接真实后端
const USE_MOCK = false;

// Mock 数据
let mockRooms = [];
let mockGroups = [];
let mockToken = null;

function getBaseUrl() {
  if (USE_MOCK) return '';
  try {
    const accountInfo = wx.getAccountInfoSync?.();
    const envVersion = accountInfo?.miniProgram?.envVersion;
    if (envVersion === 'develop' || envVersion === 'trial') {
      return DEV_BASE_URL;
    }
  } catch (e) {}
  return PROD_BASE_URL;
}

// Mock 响应
function mockResponse(url, method, data) {
  // 登录
  if (url === '/auth/login') {
    mockToken = 'mock_token_' + Date.now();
    return { accessToken: mockToken, refreshToken: 'mock_refresh', user: { openId: 'dev_1', nickName: '麻友', avatarUrl: '' } };
  }
  // 创建牌局
  if (url === '/rooms/create') {
    const room = { id: 'room_' + Date.now(), name: '麻将计分', groupId: null, status: 'active', multiplier: 1, multiplierEnabled: false, players: [{ userId: 'dev_1', avatarUrl: '', totalScore: 0 }], rounds: [], createdAt: new Date().toISOString(), closedAt: null, tableFeeEnabled: false, createdBy: 'dev_1' };
    mockRooms.push(room);
    return { roomId: room.id, scene: 'roomId=' + room.id, name: '麻将计分' };
  }
  // 牌局状态
  if (url.match(/\/rooms\/[^/]+\/status$/)) {
    const roomId = url.split('/')[1];
    const room = mockRooms.find(r => r.id === roomId);
    return { status: room ? (room.players.length >= 2 ? 'playing' : 'waiting') : 'waiting', players: room?.players || [], playerCount: room?.players.length || 1 };
  }
  // 加入牌局
  if (url.match(/\/rooms\/[^/]+\/join$/)) {
    const roomId = url.split('/')[1];
    const room = mockRooms.find(r => r.id === roomId);
    if (room) {
      ['dev_2','dev_3','dev_4'].forEach(id => {
        if (!room.players.find(p => p.userId === id)) room.players.push({ userId: id, avatarUrl: '', totalScore: 0 });
      });
    }
    return { roomId, isNewGroup: false };
  }
  // 牌局详情
  if (url.match(/^\/rooms\/[^/]+$/)) {
    const roomId = url.split('/')[1];
    const room = mockRooms.find(r => r.id === roomId);
    return room || { code: 2001, message: '牌局不存在' };
  }
  // 提交分数
  if (url.match(/\/rooms\/[^/]+\/round$/)) {
    const roomId = url.split('/')[1];
    const room = mockRooms.find(r => r.id === roomId);
    if (!room) return { code: 2001, message: '牌局不存在' };
    const { scores } = data;
    const roundNumber = room.rounds.length + 1;
    const round = { roundNumber, time: new Date().toISOString(), scores };
    room.rounds.push(round);
    Object.entries(scores).forEach(([uid, s]) => {
      const p = room.players.find(p => p.userId === uid);
      if (p) p.totalScore = (p.totalScore||0) + s;
      else room.players.push({ userId: uid, avatarUrl: '', totalScore: s });
    });
    return { round, players: room.players };
  }
  // 收盘
  if (url.match(/\/rooms\/[^/]+\/close$/)) {
    const roomId = url.split('/')[1];
    const room = mockRooms.find(r => r.id === roomId);
    if (room) { room.status = 'closed'; room.closedAt = new Date().toISOString(); }
    return { room };
  }
  // 牌局列表
  if (url === '/rooms') {
    const items = mockRooms.map(r => ({ id: r.id, name: r.name, status: r.status, players: r.players, roundCount: r.rounds.length, createdAt: r.createdAt, closedAt: r.closedAt }));
    return { items, total: items.length, page: 1, size: 20 };
  }
  // 小组
  if (url === '/groups') return { items: mockGroups, total: mockGroups.length };
  if (url.match(/^\/groups$/)) {
    const g = { id: 'g_'+Date.now(), name: data.name, members: [{ userId: 'dev_1', avatarUrl: '' }], memberCount: 1, hasActiveGame: false };
    mockGroups.push(g);
    return g;
  }
  // 默认
  return {};
}

function request(options) {
  return new Promise((resolve, reject) => {
    // Mock 模式
    if (USE_MOCK) {
      setTimeout(() => {
        const result = mockResponse(options.url, options.method, options.data);
        if (result.code && result.code >= 1000) {
          reject(result);
        } else {
          resolve(result);
        }
      }, 300);
      return;
    }

    const token = wx.getStorageSync('token');
    const app = getApp();

    wx.request({
      url: `${getBaseUrl()}${options.url}`,
      method: options.method || 'GET',
      timeout: 30000,
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header,
      },
      success: (res) => {
        if (res.statusCode === 401) {
          // 防止无限重试：最多重试 1 次
          if (options.__authRetry) {
            wx.removeStorageSync('token');
            wx.removeStorageSync('refreshToken');
            reject({ code: 1001, message: '登录已过期，请重新进入小程序' });
            return;
          }
          if (app && app.autoLogin) {
            // 先清掉过期 token，强制重新登录
            wx.removeStorageSync('token');
            wx.removeStorageSync('refreshToken');
            app.autoLogin(true).then((ok) => {
              if (ok) {
                request({ ...options, __authRetry: true }).then(resolve).catch(reject);
              } else {
                reject({ code: 1001, message: '登录失败，请重新进入小程序' });
              }
            });
            return;
          }
          wx.removeStorageSync('token');
          wx.removeStorageSync('refreshToken');
          reject({ code: 1001, message: '登录已过期' });
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
        } else {
          const err = res.data || {};
          reject({ code: err.code || res.statusCode, message: err.message || '请求失败' });
        }
      },
      fail: (err) => {
        reject({ code: -1, message: '网络异常', detail: err });
      },
    });
  });
}

module.exports = { request, getBaseUrl };
