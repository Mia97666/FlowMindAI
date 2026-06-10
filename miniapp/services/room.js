const { request } = require('../utils/request');

// 创建牌局
function createRoom(groupId) {
  return request({
    url: '/rooms/create',
    method: 'POST',
    data: { groupId },
  });
}

// 获取牌局列表
function getRooms(params = {}) {
  return request({
    url: '/rooms',
    data: {
      status: params.status,
      page: params.page || 1,
      size: params.size || 20,
    },
  });
}

// 获取牌局详情
function getRoomDetail(roomId) {
  return request({ url: `/rooms/${roomId}` });
}

// 轮询牌局状态
function getRoomStatus(roomId) {
  return request({ url: `/rooms/${roomId}/status` });
}

// 加入牌局
function joinRoom(roomId) {
  return request({
    url: `/rooms/${roomId}/join`,
    method: 'POST',
  });
}

// 提交一局分数
function submitRound(roomId, scores) {
  return request({
    url: `/rooms/${roomId}/round`,
    method: 'POST',
    data: { scores },
  });
}

// 收盘
function closeRoom(roomId) {
  return request({
    url: `/rooms/${roomId}/close`,
    method: 'PUT',
  });
}

// 更新倍率
function updateMultiplier(roomId, multiplier, enabled) {
  return request({
    url: `/rooms/${roomId}/multiplier`,
    method: 'PATCH',
    data: { multiplier, enabled },
  });
}

// 台板费开关
function toggleTableFee(roomId, enabled) {
  return request({
    url: `/rooms/${roomId}/table-fee`,
    method: 'PATCH',
    data: { enabled },
  });
}

module.exports = {
  createRoom,
  getRooms,
  getRoomDetail,
  getRoomStatus,
  joinRoom,
  submitRound,
  closeRoom,
  updateMultiplier,
  toggleTableFee,
};
