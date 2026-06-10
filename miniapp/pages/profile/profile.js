const app = getApp();
const rankingApi = require('../../services/ranking');

// ★ 四舍五入保留两位小数
function round2(num) {
  return Number(num.toFixed(2));
}

// ★ 智能格式化分数
function fmtScoreSmart(num, hasDecimal) {
  const r = round2(num);
  if (hasDecimal) {
    return (r >= 0 ? '+' : '') + r.toFixed(2);
  } else {
    const rounded = Math.round(r);
    return (rounded >= 0 ? '+' : '') + rounded;
  }
}

Page({
  data: {
    theme: 'dark', safeTop: 88,
    user: { avatar: '我', nickname: '麻将爱好者' },
    themeMode: 'system',
    todayScore: '暂无',     // 当日总分
    todayRank: null,      // 今日好友排行（数字，1=冠军）
    todayHasDecimal: false,
    todayRanking: [],       // 当日麻神排行
    totalRanking: [],       // 总计麻神排行
  },
  onLoad() {
    this.setData({ safeTop: app.globalData.safeTop || 88 });
  },
  onShow() {
    const up = app.globalData.userProfile;
    // ★ 计算当日总分
    const todayScoreInfo = this.calcTodayScore();
    this.setData({
      theme: app.globalData.resolvedTheme,
      themeMode: app.globalData.theme,
      user: up,
      todayScore: todayScoreInfo.str,
      todayHasDecimal: todayScoreInfo.hasDecimal,
    });
    // ★ 拉取麻神排行
    this.loadRanking();
  },

  // ★ 计算当日总分（从本地 allGames 中筛选今天收盘的牌局）
  calcTodayScore() {
    const MY_ID = 'p1';
    const today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD
    const closed = app.globalData.allGames.filter(g => {
      if (g.status !== 'closed') return false;
      const closedDate = (g.closedAt || '').slice(0, 10);
      return closedDate === today;
    });
    if (closed.length === 0) return { str: '暂无', hasDecimal: false };
    let sum = 0;
    let hasDecimal = false;
    closed.forEach(g => {
      const me = (g.players || []).find(p => p.id === MY_ID);
      if (me) {
        sum += me.totalScore;
        if (g.multiplierEnabled && (g.multiplier || 1) % 1 !== 0) {
          hasDecimal = true;
        }
      }
    });
    return { str: fmtScoreSmart(sum, hasDecimal), hasDecimal };
  },

  // ★ 计算今日好友排行
  calcTodayRank(todayItems) {
    if (!todayItems || todayItems.length === 0) return null;
    const myOpenId = app.globalData.userInfo?.openId;
    if (!myOpenId) return null;
    const idx = todayItems.findIndex(u => u.userId === myOpenId);
    return idx >= 0 ? idx + 1 : null;
  },

  // ★ 拉取麻神排行数据
  async loadRanking() {
    try {
      await app.autoLogin();
      const [todayResp, totalResp] = await Promise.all([
        rankingApi.getRanking('today').catch(() => ({ items: [] })),
        rankingApi.getRanking('total').catch(() => ({ items: [] })),
      ]);
      this.setData({
        todayRanking: this.formatRanking(todayResp.items || []),
        totalRanking: this.formatRanking(totalResp.items || []),
        // ★ 计算今日好友排行
        todayRank: this.calcTodayRank(todayResp.items || []),
      });
    } catch (err) {
      console.warn('[Profile] 麻神排行加载失败:', err.message);
    }
  },

  // 格式化排行数据
  formatRanking(items) {
    return items.map(item => {
      const hasDecimal = (Math.abs(item.score) % 1) !== 0;
      return {
        ...item,
        scoreStr: fmtScoreSmart(item.score, hasDecimal),
        avatar: item.nickname ? item.nickname.charAt(0) : '?',
        avatarColor: this.getAvatarColor(item.rank),
      };
    });
  },

  getAvatarColor(rank) {
    const colors = ['#D97706', '#64748B', '#B45309', '#DC2626', '#15803D', '#2563EB'];
    return colors[(rank - 1) % colors.length];
  },

  // ★ 跳转麻神排行页
  goRanking() {
    wx.navigateTo({ url: '/pages/ranking/ranking' });
  },

  onTheme(e) {
    const m = e.currentTarget.dataset.mode;
    app.setTheme(m);
    this.setData({ themeMode: m, theme: app.globalData.resolvedTheme });
  },
  onNav(e) {
    wx.navigateTo({ url: `/pages/${e.currentTarget.dataset.page}/${e.currentTarget.dataset.page}` });
  },
  onShare() { wx.showToast({ title: '分享功能', icon: 'none' }); },
  onAbout() { wx.showModal({ title: '关于麻将计分', content: '版本 1.3\n好友对局，轻松计分', showCancel: false }); },
  // Issue Fix #6: edit profile
  onEditProfile() {
    wx.showModal({
      title: '修改昵称',
      editable: true,
      content: app.globalData.userProfile.nickname,
      success: (res) => {
        if (res.confirm && res.content && res.content.trim()) {
          app.globalData.userProfile.nickname = res.content.trim();
          app.saveState();
          this.setData({ user: app.globalData.userProfile });
          wx.showToast({ title: '已更新', icon: 'success' });
        }
      },
    });
  },
  onTab(e) { wx.switchTab({ url: `/pages/${e.currentTarget.dataset.tab}/${e.currentTarget.dataset.tab}` }); },
});
