const app = getApp();
const roomApi = require('../../services/room');
const groupApi = require('../../services/group');

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

// 获取当前显示倍率
function getMult(room) {
  if (!room || !room.multiplierEnabled) return 1;
  return room.multiplier || 1;
}

Page({
  data: {
    theme: 'dark', safeTop: 88, room: null, roomName: '',
    currentTab: 'input', multEnabled: false, multiplier: 1,
    players: [], rounds: [], ranked: [],
    inputs: [], isGroup: false,
  },

  onLoad() {
    this.setData({ safeTop: app.globalData.safeTop || 88 });
    const room = app.globalData.currentRoom;
    if (!room) { wx.navigateBack(); return; }
    let displayName = room.name;
    if (room.groupId) {
      const grp = app.globalData.fixedGroups.find(x => x.id === room.groupId);
      if (grp) displayName = grp.name;
    }
    this.setData({
      theme: app.globalData.resolvedTheme,
      room, roomName: displayName,
      multEnabled: room.multiplierEnabled || false,
      multiplier: room.multiplier || 1,
      players: room.players || [],
      rounds: (room.rounds || []).map(r => ({ ...r, timeStr: fmtTime(r.time) })),
      isGroup: !!room.groupId,
    });
    this.refreshDisplay();
    this.updateInputs();
    const hasRounds = (room.rounds || []).length > 0;
    this.setData({ currentTab: hasRounds ? 'rounds' : 'input' });
  },

  // 刷新所有显示数据（应用当前倍率）
  refreshDisplay() {
    const room = this.data.room;
    if (!room) return;
    const mult = getMult(room);
    // ★ 倍率有小数才显示小数位，否则显示整数
    const hasDecimal = room.multiplierEnabled && (mult % 1 !== 0);
    const rp = (room.players || []).filter(p => !p.isBanker);
    // 排行榜 — 显示总分 × 倍率
    const sorted = [...rp].sort((a, b) => b.totalScore - a.totalScore);
    const ranked = sorted.map(p => {
      const ds = round2(p.totalScore * mult);
      return {
        id: p.id, name: p.name, color: p.color, avatar: p.avatar,
        displayScore: ds,
        displayScoreStr: fmtScoreSmart(ds, hasDecimal),
      };
    });
    // 每局详情 — 原始分 × 倍率
    const rounds = (room.rounds || []).map(r => {
      const displayScores = {};
      const displayScoreStrs = {};
      (room.players || []).forEach(p => {
        const v = round2((r.scores[p.id] || 0) * mult);
        displayScores[p.id] = v;
        displayScoreStrs[p.id] = fmtScoreSmart(v, hasDecimal);
      });
      return { ...r, displayScores, displayScoreStrs, timeStr: r.timeStr || fmtTime(r.time) };
    });
    this.setData({ ranked, rounds, hasDecimal });
  },

  updateInputs() {
    const gd = app.globalData;
    const players = this.data.room?.players || [];
    const inputs = players.map(p => {
      const inp = gd.scoringInputs[p.id] || { result: p.isBanker ? 'banker' : null, score: 0 };
      return { pid: p.id, avatar: p.avatar, color: p.color, name: p.name, isBanker: !!p.isBanker, result: inp.result, score: inp.score };
    });
    this.setData({ inputs });
  },

  onTabTap(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab === 'rounds' && (this.data.room?.rounds || []).length === 0) {
      wx.showToast({ title: '请先完成第一局计分', icon: 'none' });
      return;
    }
    this.setData({ currentTab: tab });
  },

  toggleMult() {
    const room = this.data.room;
    if (!room) return;
    room.multiplierEnabled = !room.multiplierEnabled;
    this.setData({ multEnabled: room.multiplierEnabled });
    this.refreshDisplay();
    // 异步同步倍率到后端
    this.syncMultiplierToServer(room);
  },

  onMultInput(e) {
    let raw = e.detail.value;
    // 允许数字和小数点，过滤其他字符
    raw = raw.replace(/[^\d.]/g, '');
    // 只保留第一个小数点
    const dotIdx = raw.indexOf('.');
    if (dotIdx >= 0) {
      raw = raw.substring(0, dotIdx + 1) + raw.substring(dotIdx + 1).replace(/\./g, '');
    }
    // 处理过渡状态
    if (raw === '' || raw === '.') {
      this.setData({ multiplier: raw });
      return;
    }
    const v = parseFloat(raw);
    if (!isNaN(v) && v > 0) {
      this.data.room.multiplier = v;
      this.setData({ multiplier: raw });
      this.refreshDisplay();
      // 延迟同步倍率到后端
      clearTimeout(this._multTimer);
      this._multTimer = setTimeout(() => this.syncMultiplierToServer(this.data.room), 1000);
    } else if (raw.endsWith('.') && parseFloat(raw.slice(0, -1)) >= 0) {
      // 过渡状态如 "1." "0."
      this.setData({ multiplier: raw });
    }
    // 无效值忽略，保持上次有效值
  },

  // 异步同步倍率到后端
  async syncMultiplierToServer(room) {
    const serverRoomId = room.serverRoomId;
    if (!serverRoomId) return;
    try {
      await app.autoLogin();
      await roomApi.updateMultiplier(serverRoomId, room.multiplier || 1, room.multiplierEnabled);
    } catch (err) {
      console.warn('[Scoring] 后端更新倍率失败:', err.message);
    }
  },

  onResult(e) {
    const { pid, result } = e.currentTarget.dataset;
    if (!app.globalData.scoringInputs[pid]) app.globalData.scoringInputs[pid] = { result: null, score: 0 };
    app.globalData.scoringInputs[pid].result = result;
    this.updateInputs();
    this.autoCalc();
  },

  onScoreInp(e) {
    const pid = e.currentTarget.dataset.pid;
    const v = parseInt(e.detail.value) || 0;
    if (!app.globalData.scoringInputs[pid]) app.globalData.scoringInputs[pid] = { result: null, score: 0 };
    app.globalData.scoringInputs[pid].score = v;
    this.autoCalc();
  },

  onReset(e) {
    const pid = e.currentTarget.dataset.pid;
    if (app.globalData.scoringInputs[pid]) {
      app.globalData.scoringInputs[pid].score = 0;
    }
    this.updateInputs();
  },

  autoCalc() {
    const gd = app.globalData;
    const allP = this.data.room?.players || [];
    const n = allP.length;
    if (n < 2) return;
    const filled = allP.filter(p => {
      const inp = gd.scoringInputs[p.id];
      const hasRes = p.isBanker ? true : !!(inp && inp.result);
      return hasRes && inp && inp.score > 0;
    });
    if (filled.length !== n - 1) return;
    const unfilled = allP.find(p => !filled.includes(p));
    if (!unfilled) return;
    let sum = 0;
    filled.forEach(p => {
      const inp = gd.scoringInputs[p.id];
      const sign = p.isBanker ? 1 : (inp.result === 'win' ? 1 : -1);
      sum += Math.abs(inp.score) * sign;
    });
    const target = Math.abs(sum);
    if (!gd.scoringInputs[unfilled.id]) gd.scoringInputs[unfilled.id] = { result: null, score: 0 };
    if (unfilled.isBanker) {
      gd.scoringInputs[unfilled.id].score = target;
    } else {
      gd.scoringInputs[unfilled.id].result = sum > 0 ? 'lose' : 'win';
      gd.scoringInputs[unfilled.id].score = target;
    }
    this.updateInputs();
  },

  onAddPlayer() {
    wx.showModal({
      title: '邀请新玩家',
      content: '请让新玩家扫描牌局二维码加入。\n\n牌局ID：' + (this.data.room?.id || '—'),
      showCancel: true,
      confirmText: '知道了',
      success: () => {
        wx.showToast({ title: '请扫描二维码加入', icon: 'none' });
      },
    });
  },

  onConfirm() {
    const room = this.data.room;
    if (!room) return;
    const gd = app.globalData;
    const allP = room.players;

    // 验证
    let ok = true;
    allP.forEach(p => {
      const inp = gd.scoringInputs[p.id];
      const hasRes = p.isBanker ? true : !!(inp && inp.result);
      const hasScore = inp && inp.score >= 0;
      if (!hasRes || !hasScore) ok = false;
    });
    if (!ok) { wx.showToast({ title: '请填写所有玩家的胜负和分数', icon: 'none' }); return; }

    // ★ 存原始分数（1x），不乘倍率
    const rawScores = {};
    allP.forEach(p => {
      const inp = gd.scoringInputs[p.id];
      rawScores[p.id] = Math.abs(inp.score) * (p.isBanker ? 1 : (inp.result === 'win' ? 1 : -1));
    });

    const total = Object.values(rawScores).reduce((a, b) => a + b, 0);
    if (Math.round(total) !== 0) {
      wx.showToast({ title: `分数总和不等于0！差额${total > 0 ? '+' : ''}${total}`, icon: 'none' });
      return;
    }

    // ★ 存储原始分 + 累加原始总分
    const round = { roundNumber: gd.currentRoundNum, time: new Date().toISOString(), scores: rawScores };
    room.rounds.push(round);
    allP.forEach(p => { p.totalScore += (rawScores[p.id] || 0); });
    gd.currentRoundNum++;

    // 重置输入
    gd.scoringInputs = {};
    allP.forEach(p => { gd.scoringInputs[p.id] = { result: p.isBanker ? 'banker' : null, score: 0 }; });

    // 持久化
    const idx = gd.allGames.findIndex(g => g.id === room.id);
    if (idx < 0) gd.allGames.push(room);
    else gd.allGames[idx] = room;
    app.saveState();

    // ★ 异步提交分数到后端
    this.submitRoundToServer(room, rawScores);

    // 刷新显示（应用当前倍率）
    this.refreshDisplay();
    this.updateInputs();
    this.setData({ currentTab: 'rounds' });
    wx.showToast({ title: `第${round.roundNumber}局分数已统计`, icon: 'success' });
  },

  // 提交一局分数到后端
  async submitRoundToServer(room, rawScores) {
    const serverRoomId = room.serverRoomId;
    if (!serverRoomId) return;
    try {
      await app.autoLogin();
      await roomApi.submitRound(serverRoomId, rawScores);
      console.log('[Scoring] 后端提交分数成功');
    } catch (err) {
      console.warn('[Scoring] 后端提交分数失败:', err.message);
    }
  },

  onCloseGame() {
    const room = this.data.room;
    if (!room) return;
    if (room.multiplierEnabled) {
      wx.showModal({
        title: '确认收盘倍率',
        content: String(room.multiplier || 1),
        editable: true,
        success: (res) => {
          if (res.confirm) {
            const finalMult = parseFloat(res.content) || (room.multiplier || 1);
            if (finalMult <= 0) {
              wx.showToast({ title: '倍率必须大于0', icon: 'none' });
              return;
            }
            room.multiplier = finalMult;
            this.setData({ multiplier: finalMult });
            this.doCloseGame(room, finalMult);
          }
        },
      });
    } else {
      this.doCloseGame(room, 1);
    }
  },

  // ★ 收盘时固化倍率：所有分数 × finalMult（四舍五入保留两位小数）
  doCloseGame(room, finalMult) {
    // 乘以最终倍率并保存
    (room.rounds || []).forEach(r => {
      Object.keys(r.scores).forEach(k => {
        r.scores[k] = round2(r.scores[k] * finalMult);
      });
    });
    (room.players || []).forEach(p => {
      p.totalScore = round2(p.totalScore * finalMult);
    });
    room.multiplier = finalMult;
    room.status = 'closed';
    room.closedAt = new Date().toISOString();

    const idx = app.globalData.allGames.findIndex(g => g.id === room.id);
    if (idx >= 0) app.globalData.allGames[idx] = room;
    if (room.groupId) {
      const g = app.globalData.fixedGroups.find(x => x.id === room.groupId);
      if (g) { g.hasActiveGame = false; g.activeRoomId = null; }
    }
    app.saveState();

    // ★ 异步通知后端收盘
    this.closeRoomOnServer(room, finalMult);

    wx.showToast({ title: '已收盘', icon: 'success' });
    setTimeout(() => wx.switchTab({ url: '/pages/games/games' }), 1000);
  },

  // 异步收盘到后端
  async closeRoomOnServer(room, finalMult) {
    const serverRoomId = room.serverRoomId;
    if (!serverRoomId) return;
    try {
      await app.autoLogin();
      // 先更新倍率
      if (finalMult !== 1) {
        await roomApi.updateMultiplier(serverRoomId, finalMult, true);
      }
      // 再收盘
      await roomApi.closeRoom(serverRoomId);
      console.log('[Scoring] 后端收盘成功');
    } catch (err) {
      console.warn('[Scoring] 后端收盘失败:', err.message);
    }
  },

  onSaveGroup() {
    const room = this.data.room;
    if (!room || room.groupId) return;
    wx.showModal({
      title: '为你们的小组起个名字吧',
      editable: true,
      content: '',
      success: (res) => {
        if (res.confirm && res.content && res.content.trim()) {
          const name = res.content.trim();
          const regP = room.players.filter(p => !p.isBanker);
          const gid = 'group_' + Date.now();
          app.globalData.fixedGroups.push({
            id: gid, name,
            members: regP.map(p => p.avatar),
            avatarColors: regP.map(p => p.color),
            memberIds: regP.map(p => p.id),
            hasActiveGame: true, activeRoomId: room.id,
            createdAt: new Date().toISOString(),
          });
          room.groupId = gid; room.name = name;
          app.saveState();
          this.setData({ roomName: name, isGroup: true });

          // ★ 异步同步到后端
          this.saveGroupToServer(name, room);

          wx.showToast({ title: `已保存「${name}」`, icon: 'success' });
        }
      },
    });
  },

  // 异步保存小组到后端
  async saveGroupToServer(name, room) {
    try {
      await app.autoLogin();
      const serverRoomId = room.serverRoomId;
      await groupApi.createGroup(name, serverRoomId);
      console.log('[Scoring] 后端保存小组成功');
    } catch (err) {
      console.warn('[Scoring] 后端保存小组失败:', err.message);
    }
  },

  onShare() {
    wx.showToast({ title: '📸 截图已生成', icon: 'none' });
  },

  onBack() {
    app.globalData.currentRoom = null;
    wx.navigateBack({
      delta: 1,
      fail: () => wx.switchTab({ url: '/pages/index/index' }),
    });
  },
});
