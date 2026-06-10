const app = getApp();
const rankingApi = require('../../services/ranking');

Page({
  data: {
    theme: 'dark', safeTop: 88,
    currentTab: 'today', // today | total
    todayList: [],
    totalList: [],
    loading: false,
    // 生涯弹窗
    showCareer: false,
    careerUser: null,
    careerStats: null,
  },

  onLoad() {
    this.setData({ safeTop: app.globalData.safeTop || 88 });
  },

  onShow() {
    this.setData({ theme: app.globalData.resolvedTheme });
    this.loadRanking();
  },

  // 切换 tab
  onTabTap(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
    if ((tab === 'today' && this.data.todayList.length === 0) ||
        (tab === 'total' && this.data.totalList.length === 0)) {
      this.loadRanking();
    }
  },

  // 加载排行数据
  async loadRanking() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const res = await rankingApi.getRanking();
      // res = { today: [...], total: [...] }
      const today = (res.today || []).map((u, i) => ({ ...u, rank: i + 1 }));
      const total = (res.total || []).map((u, i) => ({ ...u, rank: i + 1 }));
      this.setData({ todayList: today, totalList: total });
    } catch (err) {
      console.error('加载排行失败', err);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 点击头像查看生涯
  onAvatarTap(e) {
    const user = e.currentTarget.dataset.user;
    if (!user) return;
    // 从本地 allGames 计算生涯数据
    const stats = this.calcCareer(user.userId);
    this.setData({
      showCareer: true,
      careerUser: user,
      careerStats: stats,
    });
  },

  // 从本地数据计算生涯
  calcCareer(userId) {
    const games = app.globalData.allGames || [];
    let totalGames = 0;
    let totalWins = 0;
    let totalScore = 0;
    let maxScore = -Infinity;
    games.forEach(g => {
      if (g.status !== 'closed') return;
      const player = (g.players || []).find(p => p.id === userId);
      if (!player) return;
      totalGames++;
      if (player.totalScore > 0) totalWins++;
      totalScore += (player.totalScore || 0);
      if (player.totalScore > maxScore) maxScore = player.totalScore;
    });
    const winRate = totalGames > 0 ? Math.round(totalWins / totalGames * 100) : 0;
    return {
      totalGames,
      totalWins,
      winRate,
      totalScore: Math.round(totalScore * 100) / 100,
      maxScore: maxScore === -Infinity ? 0 : Math.round(maxScore * 100) / 100,
    };
  },

  // 关闭生涯弹窗
  onCloseCareer() {
    this.setData({ showCareer: false, careerUser: null, careerStats: null });
  },

  // 阻止冒泡
  stopPropagation() {},

  // 删除好友
  async onDeleteFriend() {
    const user = this.data.careerUser;
    if (!user) return;
    wx.showModal({
      title: '删除好友',
      content: `确定删除好友「${user.nickname}」？删除后不再出现在排行中。`,
      success: async (res) => {
        if (res.confirm) {
          try {
            await rankingApi.deleteFriend(user.userId);
            wx.showToast({ title: '已删除', icon: 'success' });
            this.setData({ showCareer: false, careerUser: null, careerStats: null });
            this.loadRanking();
          } catch (err) {
            wx.showToast({ title: '删除失败', icon: 'none' });
          }
        }
      }
    });
  },

  onGoBack() {
    wx.navigateBack();
  },
});
