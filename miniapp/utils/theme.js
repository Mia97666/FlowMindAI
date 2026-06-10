const THEME_KEY = 'theme_preference';

function getSystemTheme() {
  try {
    const sysInfo = wx.getSystemInfoSync();
    return sysInfo.theme || 'light';
  } catch (e) {
    return 'light';
  }
}

function applyTheme(theme) {
  const pages = getCurrentPages();
  pages.forEach(page => {
    if (page.setData) {
      page.setData({ theme });
    }
  });
}

function initTheme() {
  const preference = wx.getStorageSync(THEME_KEY) || 'system';
  const resolved = preference === 'system' ? getSystemTheme() : preference;
  applyTheme(resolved);
  return { preference, resolved };
}

function setTheme(mode) {
  wx.setStorageSync(THEME_KEY, mode);
  const resolved = mode === 'system' ? getSystemTheme() : mode;
  applyTheme(resolved);
  return resolved;
}

function onThemeChange(callback) {
  if (wx.onThemeChange) {
    wx.onThemeChange((res) => {
      callback(res.theme);
    });
  }
}

module.exports = { initTheme, setTheme, getSystemTheme, onThemeChange };
