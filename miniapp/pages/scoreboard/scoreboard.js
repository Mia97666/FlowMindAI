const app = getApp();

function fmtTime(iso) {
  if (!iso) return '';
  try {
    const d = new Date(iso);
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  } catch (e) { return iso || ''; }
}

// ★ 四舍五入保留两位小数（解决 JS 浮点精度问题）
function round2(num) {
  return Number(num.toFixed(2));
}

// 格式化分数为带符号的两位小数字符串
function fmtScore(num) {
  const r = round2(num);
  return (r >= 0 ? '+' : '') + r.toFixed(2);
}

// ★ 智能格式化：倍率有小数时保留两位小数，否则显示整数
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
  data: { theme: 'dark', safeTop: 88, room: null, ranked: [], rounds: [] },
  onLoad() {
    this.setData({ safeTop: app.globalData.safeTop || 88 });
    const room = app.globalData.currentRoom;
    if (!room) { wx.navigateBack(); return; }
    const rp = (room.players || []).filter(p => !p.isBanker);
    // Issue Fix #10: resolve group name dynamically
    let displayName = room.name;
    if (room.groupId) {
      const grp = app.globalData.fixedGroups.find(x => x.id === room.groupId);
      if (grp) displayName = grp.name;
    }
    // ★ 判断是否需要显示小数位
    const hasDecimal = room.multiplierEnabled && (room.multiplier % 1 !== 0);
    // ★ 格式化排行榜分数
    const ranked = [...rp].sort((a, b) => b.totalScore - a.totalScore).map(p => ({
      ...p,
      totalScore: round2(p.totalScore),
      totalScoreStr: fmtScoreSmart(p.totalScore, hasDecimal),
    }));
    // ★ 格式化每局分数
    const rounds = (room.rounds || []).map(r => {
      const scoreStrs = {};
      (room.players || []).forEach(p => {
        const v = round2(r.scores[p.id] || 0);
        scoreStrs[p.id] = fmtScoreSmart(v, hasDecimal);
      });
      return { ...r, scoreStrs, timeStr: fmtTime(r.time) };
    });
    this.setData({
      theme: app.globalData.resolvedTheme,
      room: { ...room, name: displayName, closedAtStr: fmtTime(room.closedAt) },
      ranked,
      rounds,
      hasDecimal,
    });
  },
  onShare() { wx.showToast({ title: '📸 截图', icon: 'none' }); },
  onBack() {
    wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/index/index' }) });
  },
});
