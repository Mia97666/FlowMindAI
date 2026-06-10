const app = getApp();
const groupApi = require('../../services/group');

Page({
  data: {
    theme: 'dark',
    safeTop: 88,
    groups: [],
    editing: false,
    selectedIds: [],
  },

  onLoad() {
    this.setData({ safeTop: app.globalData.safeTop || 88 });
  },

  onShow() {
    this.setData({ theme: app.globalData.resolvedTheme });
    this.load();
  },

  load() {
    this.setData({
      groups: app.globalData.fixedGroups.map(g => ({
        ...g,
        date: (g.createdAt || '').slice(0, 10),
        hasActive: g.hasActiveGame || false,
        selected: false,
      })),
      editing: false,
      selectedIds: [],
    });
  },

  onToggleEdit() {
    const editing = !this.data.editing;
    if (editing) {
      // 进入编辑模式：所有项初始未选中
      const groups = this.data.groups.map(g => ({ ...g, selected: false }));
      this.setData({ editing, groups, selectedIds: [] });
    } else {
      this.setData({ editing, selectedIds: [] });
    }
  },

  onToggleSelect(e) {
    const id = e.currentTarget.dataset.id;
    // 直接在 item 上修改 selected 状态
    const groups = this.data.groups.map(g => {
      if (g.id === id) return { ...g, selected: !g.selected };
      return g;
    });
    const selectedIds = groups.filter(g => g.selected).map(g => g.id);
    this.setData({ groups, selectedIds });
  },

  onSelectAll() {
    const allSelected = this.data.groups.every(g => g.selected);
    const groups = this.data.groups.map(g => ({ ...g, selected: !allSelected }));
    const selectedIds = groups.filter(g => g.selected).map(g => g.id);
    this.setData({ groups, selectedIds });
  },

  onDeleteSelected() {
    if (this.data.selectedIds.length === 0) {
      wx.showToast({ title: '请先选择小组', icon: 'none' });
      return;
    }
    wx.showModal({
      title: '确认删除',
      content: `确定删除选中的 ${this.data.selectedIds.length} 个小组吗？`,
      success: (res) => {
        if (res.confirm) {
          const ids = this.data.selectedIds;
          app.globalData.fixedGroups = app.globalData.fixedGroups.filter(
            g => !ids.includes(g.id)
          );
          app.saveState();
          this.load();
          // 异步删除后端
          this.deleteGroupsOnServer(ids);
          wx.showToast({ title: '已删除', icon: 'success' });
        }
      },
    });
  },

  async deleteGroupsOnServer(ids) {
    try {
      await app.autoLogin();
      // 区分后端小组和本地小组
      const serverIds = ids.filter(id => !id.startsWith('group_'));
      if (serverIds.length > 0) {
        await groupApi.deleteGroups(serverIds);
      }
      // 本地小组不需要同步后端
    } catch (err) {
      console.warn('[Groups] 后端删除失败:', err.message);
    }
  },

  onRename(e) {
    const idx = e.currentTarget.dataset.idx;
    const name = e.currentTarget.dataset.name;
    const group = app.globalData.fixedGroups[idx];
    if (!group) return;
    wx.showModal({
      title: '修改名称', editable: true, content: name,
      success: (res) => {
        if (res.confirm && res.content && res.content.trim()) {
          const newName = res.content.trim();
          group.name = newName;
          app.globalData.allGames.forEach(g => {
            if (g.groupId === group.id) { g.name = newName; }
          });
          app.saveState();
          this.load();
          // 异步同步后端
          this.renameGroupOnServer(group);
          wx.showToast({ title: '已更新', icon: 'success' });
        }
      },
    });
  },

  async renameGroupOnServer(group) {
    if (group.id.startsWith('group_')) return; // 本地小组不同步
    try {
      await app.autoLogin();
      await groupApi.updateGroup(group.id, group.name);
    } catch (err) {
      console.warn('[Groups] 后端重命名失败:', err.message);
    }
  },

  onClearAll() {
    wx.showModal({
      title: '确认清空', content: '确定清空所有固定小组吗？此操作不可恢复。',
      success: (res) => {
        if (res.confirm) {
          app.globalData.fixedGroups = [];
          app.saveState();
          this.load();
          // 异步清空后端
          this.clearGroupsOnServer();
          wx.showToast({ title: '已清空', icon: 'success' });
        }
      },
    });
  },

  async clearGroupsOnServer() {
    try {
      await app.autoLogin();
      await groupApi.clearAllGroups();
    } catch (err) {
      console.warn('[Groups] 后端清空失败:', err.message);
    }
  },

  onBack() {
    wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/index/index' }) });
  },
});
