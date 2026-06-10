const { request } = require('../utils/request');

// 获取麻神排行
function getRanking(type) {
  return request({
    url: '/ranking',
    data: { type },  // 'today' 或 'total'
  });
}

// 删除好友
function deleteFriend(friendUserId) {
  return request({
    url: '/ranking/friend/delete',
    method: 'POST',
    data: { friendUserId },
  });
}

module.exports = {
  getRanking,
  deleteFriend,
};
