# CopyParty 网盘（独立 APK）

把 **copyparty** 完整功能打包成独立安卓 App，不依赖简云主应用，可单独安装、开机自启、后台常驻。

## 功能
- ✅ **copyparty 全部原有功能保留**（多协议网盘、网页上传/预览/播放、WebDAV/FTP/SMB、共享目录管理）
- ✅ 开机自启 + 前台服务保活
- ✅ 启动 / 停止 / 实时状态 / 日志查看 / 日志分享
- ✅ 端口自定义（默认 5301）
- ✅ 简云风格 UI（橙色渐变 + 圆角卡片）

## 使用
1. 安装 APK 后打开，首次启动自动解压运行包（约 30MB，需稍等）
2. 点「启动服务」
3. 浏览器访问 `http://本机IP:5301`（copyparty 管理页，默认无密码或按提示设置）

## 构建
```bash
./gradlew assembleDebug
```
APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

## 开源
- copyparty：MIT License（https://github.com/9001/copyparty）
- python：Python Software Foundation License（termux 构建）
- 本启动器：MIT License

## 注意
copyparty 运行包来自 termux 的 python 3.14 + copyparty.pyz，通过系统 linker64 启动，需要 arm64 设备。
