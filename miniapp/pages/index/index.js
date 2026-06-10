const app = getApp();

Page({
  data: {
    theme: 'dark', safeTop: 88, scanned: [], nonScanned: [],
    tableFee: false, scanCount: 0,
    groups: [], tab: 'index',
  },

  onLoad() {
    this.setData({ safeTop: app.globalData.safeTop || 88 });
    this.reload();
  },

  onShow() {
    const gd = app.globalData;
    this.setData({
      theme: gd.resolvedTheme,
      tableFee: gd.tableFeeEnabled,
      scanned: gd.scannedPlayers || [],
      groups: this.formatGroups(gd.fixedGroups),
      tab: 'index',
    });
    this.updateScanList();
    // 异步同步后端数据
    this.syncFromServer();
  },

  // 异步从后端同步数据
  async syncFromServer() {
    try {
      await Promise.all([
        app.syncGamesFromServer(),
        app.syncGroupsFromServer(),
      ]);
      // 同步完刷新 UI
      const gd = app.globalData;
      this.setData({ groups: this.formatGroups(gd.fixedGroups) });
    } catch (err) {
      // 静默失败，不影响本地使用
    }
  },

  reload() {
    const gd = app.globalData;
    this.setData({
      theme: gd.resolvedTheme,
      tableFee: gd.tableFeeEnabled,
      scanned: gd.scannedPlayers || [],
      groups: this.formatGroups(gd.fixedGroups),
    });
    this.updateScanList();
  },

  updateScanList() {
    const scanned = this.data.scanned;
    const allPlayers = app.globalData.players;
    const nonScanned = allPlayers.filter(p => !scanned.find(s => s.id === p.id) && p.id !== 'p1');
    this.setData({
      nonScanned,
      scanCount: 1 + scanned.length,
    });
  },

  formatGroups(groups) {
    return groups.map(g => ({
      ...g,
      date: (g.createdAt || '').slice(0, 10),
    }));
  },

  simScan(e) {
    const id = e.currentTarget.dataset.id;
    const appGd = app.globalData;
    const player = appGd.players.find(p => p.id === id);
    if (!player || appGd.scannedPlayers.length >= 3) return;
    if (!appGd.scannedPlayers.find(p => p.id === id)) {
      appGd.scannedPlayers.push({ ...player });
      this.setData({ scanned: appGd.scannedPlayers });
      this.updateScanList();
    }
  },

  toggleFee() {
    const val = !app.globalData.tableFeeEnabled;
    app.globalData.tableFeeEnabled = val;
    this.setData({ tableFee: val });
  },

  onCreate() {
    if (this.data.scanCount < 2) {
      wx.showToast({ title: '至少需要两人', icon: 'none' });
      return;
    }
    app.openNewRoom();
  },

  onGroupTap(e) {
    const gid = e.currentTarget.dataset.id;
    const g = app.globalData.fixedGroups.find(x => x.id === gid);
    if (!g) return;
    if (g.hasActiveGame && g.activeRoomId) {
      const r = app.globalData.allGames.find(x => x.id === g.activeRoomId);
      if (r && r.status === 'active') {
        app.globalData.currentRoom = r;
        app.globalData.currentRoundNum = r.rounds.length + 1;
        app.globalData.scoringInputs = {};
        r.players.forEach(p => {
          app.globalData.scoringInputs[p.id] = { result: p.isBanker ? 'banker' : null, score: 0 };
        });
        wx.navigateTo({ url: '/pages/scoring/scoring' });
        return;
      }
      g.hasActiveGame = false; g.activeRoomId = null; app.saveState();
    }
    app.globalData.tableFeeEnabled = false;
    app.openNewRoom(gid);
  },

  onManage() {
    wx.navigateTo({ url: '/pages/groups/groups' });
  },

  onTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab === 'index') return;
    wx.switchTab({ url: `/pages/${tab}/${tab}` });
  },
});
