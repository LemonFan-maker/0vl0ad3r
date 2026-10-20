# 0vl0ad3r

在 Android 上管理 Arch Linux 软件包的 pacman 前端，运行于 [Gh0stra1n](https://github.com/LemonFan-maker/Gh0stra1n) 提供的 Root/OverlayFS 环境。

## 功能

- Release 固定软件源、索引缓存与 SHA-256 校验
- 依赖解析、安装/更新/卸载队列
- 真实 pacman 本地数据库与系统包只读展示
- 签名校验和包名白名单保护的 AIDL 通信

## 环境要求

- Android 9（API 28）及以上，arm64-v8a
- 已安装 Gh0stra1n 1.1.6 或更高版本
- 已获取 Root 权限

## 构建

需要 JDK 17 和 Android SDK 36。

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

发布包必须使用固定的 release keystore，可通过 `RELEASE_STORE_FILE`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS` 和 `RELEASE_KEY_PASSWORD` 配置。AIDL 鉴权依赖客户端与 Gh0stra1n 使用同一签名证书，不要使用 debug 签名发布。

## 许可证

[Apache-2.0](LICENSE)
