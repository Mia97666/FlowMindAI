// 玩家默认颜色方案（与 HTML 原型一致）
const PLAYER_COLORS = ['#DC2626', '#15803D', '#2563EB', '#D97706'];

// 玩家默认头像文字
const PLAYER_TEXTS = ['東', '南', '西', '北'];

// 台板费（每人每局扣 50 分）
const TABLE_FEE = 50;

// 默认倍率
const DEFAULT_MULTIPLIER = 1.0;

// 分页大小
const PAGE_SIZE = 20;

// 二维码轮询间隔（毫秒）
const QR_POLL_INTERVAL = 2000;

// 二维码轮询超时（毫秒）
const QR_POLL_TIMEOUT = 5 * 60 * 1000;

module.exports = {
  PLAYER_COLORS,
  PLAYER_TEXTS,
  TABLE_FEE,
  DEFAULT_MULTIPLIER,
  PAGE_SIZE,
  QR_POLL_INTERVAL,
  QR_POLL_TIMEOUT,
};
