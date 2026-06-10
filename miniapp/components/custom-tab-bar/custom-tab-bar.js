Component({
  data: { selected: 0 },
  methods: {
    onTap(e) {
      const idx = Number(e.currentTarget.dataset.index);
      const urls = ['/pages/index/index', '/pages/games/games', '/pages/profile/profile'];
      wx.switchTab({ url: urls[idx] });
    },
  },
});
