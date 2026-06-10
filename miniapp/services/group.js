const { request } = require('../utils/request');

// 获取小组列表
function getGroups() {
  return request({ url: '/groups' });
}

// 创建小组
function createGroup(name, roomId) {
  return request({
    url: '/groups',
    method: 'POST',
    data: { name, roomId },
  });
}

// 更新小组名称
function updateGroup(id, name) {
  return request({
    url: `/groups/${id}`,
    method: 'PATCH',
    data: { name },
  });
}

// 批量删除小组
function deleteGroups(ids) {
  return request({
    url: '/groups/batch',
    method: 'DELETE',
    data: { ids },
  });
}

// 清空所有小组
function clearAllGroups() {
  return request({
    url: '/groups/all',
    method: 'DELETE',
  });
}

module.exports = {
  getGroups,
  createGroup,
  updateGroup,
  deleteGroups,
  clearAllGroups,
};
