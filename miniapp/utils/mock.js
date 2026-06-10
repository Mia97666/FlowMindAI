// ============================================================
// 麻将计分 — Mock 数据与演示数据
// 严格参考 WeChatProjects/index.html 的 STATE 模型
// ============================================================

// 4 个固定玩家
const PLAYERS = [
  { id: 'p1', name: '东', color: '#DC2626', avatar: '東', totalScore: 0 },
  { id: 'p2', name: '南', color: '#15803D', avatar: '南', totalScore: 0 },
  { id: 'p3', name: '西', color: '#2563EB', avatar: '西', totalScore: 0 },
  { id: 'p4', name: '北', color: '#D97706', avatar: '北', totalScore: 0 },
];

const TABLE_BANKER = { id: 'p5', name: '台板', color: '#7C3AED', avatar: '台', totalScore: 0, isBanker: true };

// 演示数据
function getDemoData() {
  const demoGroup1 = {
    id: 'group_demo1', name: '周末麻将局',
    members: ['東', '南', '西', '北'],
    avatarColors: ['#DC2626', '#15803D', '#2563EB', '#D97706'],
    memberIds: ['p1', 'p2', 'p3', 'p4'],
    hasActiveGame: false, activeRoomId: null,
    createdAt: '2026-05-15T10:00:00Z',
  };
  const demoGroup2 = {
    id: 'group_demo2', name: '邻居牌友圈',
    members: ['東', '南', '西', '北'],
    avatarColors: ['#DC2626', '#15803D', '#2563EB', '#D97706'],
    memberIds: ['p1', 'p2', 'p3', 'p4'],
    hasActiveGame: false, activeRoomId: null,
    createdAt: '2026-04-20T08:00:00Z',
  };
  const demoGame = {
    id: 'room_old1', name: '周末麻将局', groupId: 'group_demo1',
    status: 'closed', multiplier: 1, multiplierEnabled: false, tableFee: false,
    createdAt: '2026-05-15T14:00:00Z', closedAt: '2026-05-15T18:30:00Z',
    players: [
      { id: 'p1', name: '东', color: '#DC2626', avatar: '東', totalScore: 15 },
      { id: 'p2', name: '南', color: '#15803D', avatar: '南', totalScore: 5 },
      { id: 'p3', name: '西', color: '#2563EB', avatar: '西', totalScore: -8 },
      { id: 'p4', name: '北', color: '#D97706', avatar: '北', totalScore: -12 },
    ],
    rounds: [
      { roundNumber: 1, time: '2026-05-15T14:30:00Z', scores: { p1: 8, p2: -4, p3: 3, p4: -7 } },
      { roundNumber: 2, time: '2026-05-15T14:45:00Z', scores: { p1: -2, p2: 10, p3: -5, p4: -3 } },
      { roundNumber: 3, time: '2026-05-15T15:10:00Z', scores: { p1: 12, p2: -6, p3: -4, p4: -2 } },
      { roundNumber: 4, time: '2026-05-15T15:35:00Z', scores: { p1: -3, p2: 5, p3: -2, p4: 0 } },
    ],
  };
  return { groups: [demoGroup1, demoGroup2], games: [demoGame] };
}

module.exports = { PLAYERS, TABLE_BANKER, getDemoData };
