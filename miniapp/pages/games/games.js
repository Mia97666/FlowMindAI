const app = getApp();

function fmtTime(iso) {
  if (!iso) return '';
  try {
    const d = new Date(iso);
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch (e) { return iso || ''; }
}

// ★ 四舍五入保留两位小数
function round2(num) {
  return Number(num.toFixed(2));
}

// ★ 智能格式化：进行中=整数；已结束=根据倍率决定
function fmtScoreSmart(totalScore, isActive, hasDecimal) {
  const r = round2(totalScore);
  if (isActive) {
    // 进行中：强制整数
    return (r >= 0 ? '+' : '') + Math.round(r);
  } else {
    // 已结束：根据倍率是否有小数决定
    if (hasDecimal) {
      return (r >= 0 ? '+' : '') + r.toFixed(2);
    } else {
      const rounded = Math.round(r);
      return (rounded >= 0 ? '+' : '') + rounded;
    }
  }
}

// 获取某房间的倍率是否有小数
function roomHasDecimal(game) {
  if (!game) return false;
  const mult = Number(game.multiplier) || 1;
  const enabled = game.multiplierEnabled !== false;
  return enabled && (mult % 1 !== 0);
}

Page({
  data: {
    theme: 'dark', safeTop: 88,
    filter: 'active',  // active | closed
    filterYear: '',     // 筛选年份，空=不限
    filterMonth: '',    // 筛选月份，空=不限
    yearOptions: [],    // 年份选项
    monthOptions: [],   // 月份选项（1-12）
    list: [],
  },

  onLoad() {
    this.setData({ safeTop: app.globalData.safeTop || 88 });
    this.initFilters();
  },

  onShow() {
    this.setData({ theme: app.globalData.resolvedTheme });
    this.initFilters();
    this.loadList();
  },

  // ★ 初始化年/月筛选选项
  initFilters() {
    const games = app.globalData.allGames || [];
    const years = new Set();
    games.forEach(g => {
      const d = new Date(g.createdAt || g.closedAt);
      const t = d.getTime();
      if (!isNaN(t)) years.add(d.getFullYear());
    });
    const yearOptions = Array.from(years).sort((a, b) => b - a).map(y => String(y));
    const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1));
    this.setData({
      yearOptions: ['全部年份'].concat(yearOptions),
      monthOptions: ['全部月份'].concat(monthOptions),
    });
  },

  loadList() {
    const { filter, filterYear, filterMonth } = this.data;
    const isActive = filter === 'active';
    const resolveName = (g) => {
      if (g.groupId) {
        const grp = app.globalData.fixedGroups.find(x => x.id === g.groupId);
        if (grp) return grp.name;
      }
      return g.name;
    };
    let games = app.globalData.allGames.filter(g => g.status === filter);

    // ★ 年/月筛选
    if (filterYear) {
      games = games.filter(g => {
        const d = new Date(g.createdAt || g.closedAt);
        return d.getFullYear() === Number(filterYear);
      });
    }
    if (filterMonth) {
      games = games.filter(g => {
        const d = new Date(g.createdAt || g.closedAt);
        return (d.getMonth() + 1) === Number(filterMonth);
      });
    }

    // ★ 排序：最新在前
    games.sort((a, b) => {
      const dateA = filter === 'closed' ? (a.closedAt || a.createdAt) : a.createdAt;
      const dateB = filter === 'closed' ? (b.closedAt || b.createdAt) : b.createdAt;
      return (dateB || '').localeCompare(dateA || '');
    });

    const hasDecimalMap = {};
    games.forEach(g => { hasDecimalMap[g.id] = roomHasDecimal(g); });

    this.setData({
      filter,
      list: games.map(g => {
        const hasDecimal = hasDecimalMap[g.id];
        return {
          id: g.id, name: resolveName(g), status: g.status,
          displayPlayers: (g.players || []).filter(p => !p.isBanker).map(p => ({
            ...p,
            totalScoreStr: fmtScoreSmart(p.totalScore, isActive, hasDecimal),
          })),
          roundCount: (g.rounds || []).length,
          timeStr: g.status === 'closed'
            ? '结束于 ' + fmtTime(g.closedAt)
            : '开始于 ' + fmtTime(g.createdAt),
        };
      }),
    });
  },

  onFilter(e) {
    const f = e.currentTarget.dataset.filter;
    this.setData({ filter: f });
    this.loadList();
  },

  onYearChange(e) {
    const val = this.data.yearOptions[e.detail.value];
    this.setData({ filterYear: val === '全部年份' ? '' : val });
    this.loadList();
  },

  onMonthChange(e) {
    const val = this.data.monthOptions[e.detail.value];
    this.setData({ filterMonth: val === '全部月份' ? '' : val });
    this.loadList();
  },

  onResetFilter() {
    this.setData({ filterYear: '', filterMonth: '' });
    this.loadList();
  },

  onTap(e) {
    const { id, status } = e.currentTarget.dataset;
    const game = app.globalData.allGames.find(g => g.id === id);
    if (!game) return;
    if (status === 'active') {
      app.globalData.currentRoom = game;
      app.globalData.currentRoundNum = (game.rounds || []).length + 1;
      app.globalData.scoringInputs = {};
      (game.players || []).forEach(p => { app.globalData.scoringInputs[p.id] = { result: p.isBanker ? 'banker' : null, score: 0 }; });
      wx.navigateTo({ url: '/pages/scoring/scoring' });
    } else {
      app.globalData.currentRoom = game;
      wx.navigateTo({ url: '/pages/scoreboard/scoreboard' });
    }
  },

  onTab(e) {
    const tab = e.currentTarget.dataset.tab;
    wx.switchTab({ url: `/pages/${tab}/${tab}` });
  },
});
