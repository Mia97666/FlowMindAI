const { PLAYERS, TABLE_BANKER, getDemoData } = require('./utils/mock');
const { login, isLoggedIn } = require('./utils/auth');
const roomApi = require('./services/room');
const groupApi = require('./services/group');

App({
  globalData: {
    theme: 'system',
    resolvedTheme: 'light',
    safeTop: 88,
    tableFeeEnabled: false,
    userProfile: { nickname: '麻将爱好者', avatar: '我' },
    players: PLAYERS,
    tableBanker: TABLE_BANKER,
    currentRoom: null,
    scannedPlayers: [],
    fixedGroups: [],
    allGames: [],
    scoringInputs: {},
    currentRoundNum: 1,
    currentPage: 'home',
    pageStack: [],
    _loginPromise: null,
  },

  onLaunch() {
    // 计算安全区域顶部距离
    try {
      const capsule = wx.getMenuButtonBoundingClientRect();
      this.globalData.safeTop = capsule.bottom + 8;
    } catch (e) {
      const sys = wx.getSystemInfoSync();
      this.globalData.safeTop = (sys.statusBarHeight || 44) + 48;
    }
    // 加载持久化数据
    this.loadState();
    // 注入演示数据（仅首次）
    const demo = getDemoData();
    if (this.globalData.fixedGroups.length === 0) {
      this.globalData.fixedGroups = demo.groups;
      this.globalData.allGames = demo.games;
      this.saveState();
    }
    // 解析主题
    this.resolveTheme();
    // 监听系统主题变化
    wx.onThemeChange?.(() => {
      if (this.globalData.theme === 'system') {
        this.setTheme('system');
      }
    });
    // 自动登录后端
    this.autoLogin();
  },

  // 自动登录：确保后端 token 可用
  // force=true 时强制重新登录（用于 token 过期后）
  async autoLogin(force = false) {
    if (!force && this.globalData._loginPromise) {
      return this.globalData._loginPromise;
    }
    this.globalData._loginPromise = new Promise(async (resolve) => {
      try {
        if (force || !isLoggedIn()) {
          await login();
          console.log('[App] 自动登录成功');
        }
        resolve(true);
      } catch (err) {
        console.error('[App] 自动登录失败:', err);
        resolve(false);
      } finally {
        // 无论成功失败，完成后都清空缓存，以便下次能重新触发
        this.globalData._loginPromise = null;
      }
    });
    return this.globalData._loginPromise;
  },

  loadState() {
    try {
      const s = wx.getStorageSync('mahjong_state') || {};
      if (s.fixedGroups) this.globalData.fixedGroups = s.fixedGroups;
      if (s.allGames) this.globalData.allGames = s.allGames;
      if (s.theme) this.globalData.theme = s.theme;
      if (s.userProfile) this.globalData.userProfile = s.userProfile;
    } catch (e) {}
  },

  saveState() {
    try {
      wx.setStorageSync('mahjong_state', JSON.stringify({
        fixedGroups: this.globalData.fixedGroups,
        allGames: this.globalData.allGames,
        theme: this.globalData.theme,
        userProfile: this.globalData.userProfile,
      }));
    } catch (e) {}
  },

  resolveTheme() {
    const t = this.globalData.theme;
    if (t === 'system') {
      const sys = wx.getSystemInfoSync();
      this.globalData.resolvedTheme = (sys.theme === 'dark') ? 'dark' : 'light';
    } else {
      this.globalData.resolvedTheme = t;
    }
    this.updateThemePages();
  },

  setTheme(mode) {
    this.globalData.theme = mode;
    this.resolveTheme();
    this.saveState();
  },

  updateThemePages() {
    const pages = getCurrentPages();
    pages.forEach(page => {
      if (page.setData) {
        page.setData({ theme: this.globalData.resolvedTheme });
      }
    });
  },

  navigateTo(page, data) {
    this.globalData.currentPage = page;
    if (page === 'scoring') {
      wx.navigateTo({ url: `/pages/scoring/scoring${data ? '?data=1' : ''}` });
    } else if (page === 'groups') {
      wx.navigateTo({ url: '/pages/groups/groups' });
    } else if (page === 'stats') {
      wx.navigateTo({ url: '/pages/stats/stats' });
    } else if (page === 'scoreboard') {
      wx.navigateTo({ url: '/pages/scoreboard/scoreboard' });
    }
  },

  goBack() {
    wx.navigateBack();
  },

  // 创建牌局 — 同时调后端 API
  async openNewRoom(groupId) {
    const dt = new Date().toISOString();
    const group = this.globalData.fixedGroups.find(g => g.id === groupId);
    const rname = group ? group.name : '临时小组';
    const useFee = this.globalData.tableFeeEnabled;

    const basePlayers = this.globalData.players.map(p => ({ ...p, totalScore: 0 }));
    const allPlayers = useFee ? [...basePlayers, { ...this.globalData.tableBanker, totalScore: 0 }] : basePlayers;

    // 先创建本地 room
    const localRoomId = 'room_' + Date.now();
    this.globalData.currentRoom = {
      id: localRoomId, name: rname, groupId: groupId || null, tableFee: useFee,
      multiplier: 1, multiplierEnabled: false,
      rounds: [], players: allPlayers, status: 'active',
      createdAt: dt, closedAt: null,
      serverRoomId: null,  // 后端返回的真实 ID
    };
    this.globalData.scoringInputs = {};
    allPlayers.forEach(p => {
      this.globalData.scoringInputs[p.id] = {
        result: p.isBanker ? 'banker' : null,
        score: 0
      };
    });
    this.globalData.currentRoundNum = 1;

    // 尝试调后端 API 创建
    try {
      await this.autoLogin();
      const resp = await roomApi.createRoom(groupId);
      if (resp && resp.roomId) {
        this.globalData.currentRoom.serverRoomId = resp.roomId;
        console.log('[App] 后端创建牌局成功:', resp.roomId);
      }
    } catch (err) {
      console.warn('[App] 后端创建牌局失败，使用本地模式:', err.message);
    }

    if (groupId) {
      const g = this.globalData.fixedGroups.find(x => x.id === groupId);
      if (g) { g.hasActiveGame = true; g.activeRoomId = localRoomId; this.saveState(); }
    }
    this.globalData.scannedPlayers = [];
    this.navigateTo('scoring');
  },

  // 从后端同步牌局列表到本地
  async syncGamesFromServer() {
    try {
      await this.autoLogin();
      const resp = await roomApi.getRooms({ size: 50 });
      if (resp && resp.items) {
        // 将后端数据转换为前端格式，合并到 allGames
        const serverGames = resp.items.map(r => ({
          id: r.id,
          name: r.name,
          groupId: r.groupId,
          status: r.status,
          multiplier: r.multiplier,
          multiplierEnabled: r.multiplierEnabled,
          createdAt: r.createdAt,
          closedAt: r.closedAt,
          players: r.players,
          roundCount: r.roundCount,
          isServerData: true,
        }));
        // 合并：后端数据优先
        const localIds = new Set(serverGames.map(g => g.id));
        const localOnly = this.globalData.allGames.filter(g => !localIds.has(g.id));
        this.globalData.allGames = [...serverGames, ...localOnly];
        this.saveState();
        console.log('[App] 同步牌局列表:', serverGames.length, '条');
      }
    } catch (err) {
      console.warn('[App] 同步牌局列表失败:', err.message);
    }
  },

  // 从后端同步小组列表
  async syncGroupsFromServer() {
    try {
      await this.autoLogin();
      const resp = await groupApi.getGroups();
      if (resp && resp.items) {
        const serverGroups = resp.items.map(g => ({
          id: g.id,
          name: g.name,
          members: g.members ? g.members.map(m => m.userId || m) : [],
          memberIds: g.members ? g.members.map(m => m.userId || m.id) : [],
          avatarColors: ['#DC2626', '#15803D', '#2563EB', '#D97706'],
          memberCount: g.memberCount,
          hasActiveGame: g.hasActiveGame,
          activeRoomId: g.activeRoomId,
          createdAt: g.createdAt,
          isServerData: true,
        }));
        const serverIds = new Set(serverGroups.map(g => g.id));
        const localOnly = this.globalData.fixedGroups.filter(g => !serverIds.has(g.id));
        this.globalData.fixedGroups = [...serverGroups, ...localOnly];
        this.saveState();
        console.log('[App] 同步小组列表:', serverGroups.length, '条');
      }
    } catch (err) {
      console.warn('[App] 同步小组列表失败:', err.message);
    }
  },
});
