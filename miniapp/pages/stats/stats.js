const app = getApp();

function fmtTime(iso) {
  if (!iso) return '';
  try {
    const d = new Date(iso);
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch (e) { return iso || ''; }
}

function round2(num) { return Number(num.toFixed(2)); }

function fmtScore(num) {
  const r = round2(num);
  return (r >= 0 ? '+' : '') + r.toFixed(2);
}

// ★ 智能格式化：倍率有小数时保留两位，否则整数
function fmtScoreSmart(num, hasDecimal) {
  const r = round2(num);
  if (hasDecimal) {
    return (r >= 0 ? '+' : '') + r.toFixed(2);
  }
  const rounded = Math.round(r);
  return (rounded >= 0 ? '+' : '') + rounded;
}

Page({
  data: {
    theme: 'dark', safeTop: 88,
    filterYear: '',      // 筛选年份，空=当年
    filterMonth: '',     // 筛选月份，空=不限
    yearOptions: [],
    monthOptions: [],
    stats: { total: 0, wins: 0, rate: 0, sum: 0, sumStr: '+0', highestScore: 0, highestScoreStr: '+0' },
    history: [],
    historySort: 'date',  // date | score
    footerMsg: '',
    hasDecimal: false,    // 当前筛选范围内是否有小数倍率
  },

  onLoad() {
    this.setData({ safeTop: app.globalData.safeTop || 88 });
  },

  onShow() {
    this.setData({ theme: app.globalData.resolvedTheme });
    this.initFilters();
    this.calcStats();
  },

  // ★ 初始化年/月筛选选项
  initFilters() {
    const games = app.globalData.allGames || [];
    const years = new Set();
    games.forEach(g => {
      const d = new Date(g.closedAt || g.createdAt);
      const t = d.getTime();
      if (!isNaN(t)) years.add(d.getFullYear());
    });
    const yearOptions = Array.from(years).sort((a, b) => b - a).map(y => String(y));
    const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1));
    // 默认当年
    const now = new Date();
    const defaultYear = String(now.getFullYear());
    this.setData({
      yearOptions: ['全部年份'].concat(yearOptions),
      monthOptions: ['全部月份'].concat(monthOptions),
      filterYear: this.data.filterYear || defaultYear,
    });
  },

  // ★ 核心统计逻辑
  calcStats() {
    const MY_ID = 'p1';
    const { filterYear, filterMonth } = this.data;

    let closed = app.globalData.allGames.filter(g => g.status === 'closed');

    // 年份筛选
    if (filterYear) {
      closed = closed.filter(g => {
        const d = new Date(g.closedAt || g.createdAt);
        return d.getFullYear() === Number(filterYear);
      });
    }
    // 月份筛选
    if (filterMonth) {
      closed = closed.filter(g => {
        const d = new Date(g.closedAt || g.createdAt);
        return (d.getMonth() + 1) === Number(filterMonth);
      });
    }

    let wins = 0, sum = 0, highestScore = 0, hasDecimal = false;
    const historyData = [];

    closed.forEach(g => {
      const me = (g.players || []).find(p => p.id === MY_ID);
      if (!me) return;
      const rp = g.players.filter(p => !p.isBanker);
      const sorted = [...rp].sort((a, b) => b.totalScore - a.totalScore);
      if (sorted[0] && sorted[0].id === MY_ID && sorted[0].totalScore > 0) wins++;

      // 倍率处理
      const mult = Number(g.multiplier) || 1;
      const multEnabled = g.multiplierEnabled !== false;
      const gameHasDecimal = multEnabled && (mult % 1 !== 0);
      if (gameHasDecimal) hasDecimal = true;

      const myScore = round2(me.totalScore * (multEnabled ? mult : 1));
      sum += myScore;
      if (myScore > highestScore) highestScore = myScore;

      let displayName = g.name;
      if (g.groupId) {
        const grp = app.globalData.fixedGroups.find(x => x.id === g.groupId);
        if (grp) displayName = grp.name;
      }

      historyData.push({
        id: g.id,
        name: displayName,
        rounds: (g.rounds || []).length,
        date: fmtTime(g.closedAt),
        closedAt: g.closedAt || '',
        myScore,
        myScoreStr: fmtScoreSmart(myScore, gameHasDecimal),
        multEnabled,
        multiplier: g.multiplier || 1,
        gameHasDecimal,
      });
    });

    // ★ 排序
    const sortFn = this.data.historySort === 'score'
      ? (a, b) => b.myScore - a.myScore
      : (a, b) => (b.closedAt || '').localeCompare(a.closedAt || '');
    historyData.sort(sortFn);

    // ★ 总分格式化
    const sumVal = round2(sum);
    const sumStr = fmtScoreSmart(sumVal, hasDecimal);

    // ★ 底部调皮语
    const footerMsg = this.getFooterMsg(sumVal);

    this.setData({
      stats: {
        total: closed.length,
        wins,
        rate: closed.length > 0 ? Math.round(wins / closed.length * 100) : 0,
        sum: sumVal,
        sumStr,
        highestScore: round2(highestScore),
        highestScoreStr: fmtScoreSmart(highestScore, hasDecimal),
      },
      history: historyData,
      hasDecimal,
      footerMsg,
    });
  },

  // ★ 底部提示语
  getFooterMsg(sum) {
    if (sum < -200) return '💪 牌运有时，别气馁！下一局翻盘！';
    if (sum < -100) return '😄 输赢乃兵家常事，再来！';
    if (sum < 0) return '🔥 加油，再接再厉！';
    if (sum === 0) return '🤝 不赢不输，和平局！';
    if (sum < 50) return '👍 小有斩获，不错哟！';
    if (sum < 100) return '🎉 手气不错，继续保持！';
    if (sum < 200) return '🏆 麻神驾到，谁与争锋！';
    return '👑 麻将之王！无人能敌！';
  },

  onYearChange(e) {
    const val = this.data.yearOptions[e.detail.value];
    this.setData({ filterYear: val === '全部年份' ? '' : val });
    this.calcStats();
  },

  onMonthChange(e) {
    const val = this.data.monthOptions[e.detail.value];
    this.setData({ filterMonth: val === '全部月份' ? '' : val });
    this.calcStats();
  },

  onResetFilter() {
    const now = new Date();
    this.setData({ filterYear: String(now.getFullYear()), filterMonth: '' });
    this.calcStats();
  },

  // ★ 排序切换
  onSortChange(e) {
    const sort = e.currentTarget.dataset.sort;
    this.setData({ historySort: sort });
    this.calcStats();
  },

  onTap(e) {
    const g = app.globalData.allGames.find(x => x.id === e.currentTarget.dataset.id);
    if (g) { app.globalData.currentRoom = g; wx.navigateTo({ url: '/pages/scoreboard/scoreboard' }); }
  },

  onBack() {
    wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/index/index' }) });
  },

  // ★ 分享给微信好友
  onShareAppMessage() {
    const { stats } = this.data;
    return {
      title: `麻将计分 — 我的战绩：总局数${stats.total}，胜率${stats.rate}%，总分${stats.sumStr}`,
      path: '/pages/index/index',
    };
  },

  // ★ 分享按钮
  onShare() {
    // 触发系统分享菜单
    // 小程序中无法代码直接调起分享，需要用户点右上角...或button open-type="share"
    // 这里仅做提示
  },
});
