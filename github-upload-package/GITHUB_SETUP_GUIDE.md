# 🚀 GitHub云端同步设置指南

## 📋 概述

MyApplication3使用GitHub Gist作为云端数据存储，实现用户数据的跨设备同步。本指南将帮助您快速配置GitHub云端同步功能。

## 🔑 第一步：获取GitHub Personal Access Token

### 1.1 登录GitHub
访问 [GitHub](https://github.com) 并登录您的账户

### 1.2 进入设置页面
1. 点击右上角头像
2. 选择 "Settings"

### 1.3 创建Personal Access Token
1. 在左侧菜单中点击 "Developer settings"
2. 点击 "Personal access tokens"
3. 选择 "Tokens (classic)"
4. 点击 "Generate new token (classic)"

### 1.4 配置Token
1. **Note**: 填写描述，如 "MyApplication3 Data Sync"
2. **Expiration**: 建议选择 "No expiration"
3. **Select scopes**: 仅勾选 `gist` 权限
4. 点击 "Generate token"

### 1.5 保存Token
⚠️ **重要**: Token只会显示一次，请立即复制并保存到安全的地方！

## 📱 第二步：在Android应用中配置

### 2.1 打开配置界面
1. 启动MyApplication3应用
2. 进入 "设置" 或 "账户设置"
3. 找到 "GitHub云端同步" 选项

### 2.2 输入配置信息
1. **GitHub用户名**: 输入您的GitHub用户名
2. **Personal Access Token**: 粘贴刚才复制的Token
3. 点击 "保存配置"

### 2.3 验证配置
配置成功后，您会看到：
- ✅ GitHub已配置
- 自动创建私有Gist用于数据存储
- 开始同步用户数据

## 🔄 第三步：数据同步

### 3.1 自动同步
配置完成后，应用会自动：
- 将当前用户数据上传到GitHub Gist
- 定期同步学习记录和错题数据
- 在应用启动时检查数据更新

### 3.2 手动同步
您也可以手动触发同步：
1. 进入设置页面
2. 点击 "立即同步" 按钮
3. 等待同步完成

### 3.3 查看同步状态
在设置页面可以查看：
- 最后同步时间
- 同步状态（成功/失败）
- 数据版本信息

## 🌐 第四步：跨设备使用

### 4.1 在新设备上配置
1. 在新设备上安装MyApplication3
2. 使用相同的GitHub用户名和Token配置
3. 应用会自动下载云端数据

### 4.2 数据冲突处理
如果多设备同时修改数据：
- 应用会智能合并数据
- 优先保留最新的修改
- 确保数据不会丢失

## 📊 数据存储结构

### GitHub Gist文件
配置完成后，会在您的GitHub账户中创建一个私有Gist，包含：

```
MyApplication3 User Data - {您的用户名}
├── user_data.json          # 用户基本信息
├── study_records.json      # 学习记录
├── error_records.json      # 错题记录
└── app_settings.json       # 应用设置
```

### 数据安全
- 所有数据以私有Gist形式存储
- 仅您本人可以访问
- 支持版本历史记录
- 可随时删除或导出

## 🔧 高级设置

### 同步频率
- 默认：应用启动时检查更新
- 数据修改后立即同步
- 可在设置中调整同步策略

### 离线模式
- 网络不可用时使用本地缓存
- 网络恢复后自动同步
- 确保数据不会丢失

### 数据备份
- GitHub Gist自动保存历史版本
- 可手动导出数据文件
- 支持数据恢复功能

## 🆘 故障排除

### Token无效
**问题**: 提示Token无效或过期
**解决**: 
1. 检查Token是否正确复制
2. 确认Token具有gist权限
3. 重新生成新的Token

### 同步失败
**问题**: 数据同步失败
**解决**:
1. 检查网络连接
2. 验证GitHub服务状态
3. 重新配置Token

### 数据不一致
**问题**: 不同设备数据不同步
**解决**:
1. 确保使用相同GitHub账户
2. 手动触发同步
3. 检查最后同步时间

### 权限错误
**问题**: 提示权限不足
**解决**:
1. 确认Token具有gist权限
2. 检查GitHub账户状态
3. 重新生成Token

## 📞 获取帮助

### 常用链接
- [GitHub Personal Access Tokens](https://github.com/settings/tokens)
- [GitHub API文档](https://docs.github.com/en/rest/gists)
- [GitHub状态页面](https://www.githubstatus.com/)

### 联系支持
如果遇到问题：
1. 查看应用内的错误信息
2. 检查网络连接状态
3. 参考本指南的故障排除部分

## ✅ 配置完成检查清单

- [ ] 获取GitHub Personal Access Token
- [ ] Token具有gist权限
- [ ] 在应用中配置GitHub用户名和Token
- [ ] 看到"GitHub已配置"状态
- [ ] 成功创建私有Gist
- [ ] 用户数据已上传到云端
- [ ] 在其他设备上测试数据同步

---

**🎉 恭喜！您已成功配置GitHub云端同步功能！**

现在您的学习数据将安全地存储在GitHub云端，永不丢失。
