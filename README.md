# brave课表

brave课表是基于拾光课程表的无广告 Android 课表项目。导入过的课表和考试安排可以离线查看。西安工业大学版包含学校统一认证、考试导入和倒计时。

## 从其他学校导入课表

1. 在周课表点右上角的下载图标，再点「粘贴教务网址」。
2. 粘贴浏览器里学校教务页的 HTTP 或 HTTPS 地址。App 在本机匹配内置适配。找到多个时，请选择与你的学校和学籍阶段对应的适配。
3. 在学校页面用自己的账号登录，打开课表页，再点「执行导入」。可以为不同学校或学期分别新建课表。

网址只用于查找已有适配，App 使用适配配置的学校入口。未匹配到域名时，可以先按校名查找。校名也找不到时，需要新增适配。考试导入目前使用西安工业大学的教务流程。

## 开发和新增学校

Android 构建需要 JDK 21、Android SDK 平台 37 和 Build Tools 36.0.0。安装后在仓库根目录运行：

```sh
./gradlew :androidApp:assembleDebug
```

Windows 使用 `gradlew.bat :androidApp:assembleDebug`。APK 输出在 `androidApp/build/outputs/apk/debug/`，按 CPU 架构拆分。

学校匹配使用 `shared/assets/offline_repo/index/school_index.pb` 中的导入地址和学校信息。适配脚本在同目录下的 `schools/resources/`。如果要新增学校，欢迎在 Issues 提供校名、学历阶段、无需登录即可分享的官方教务入口，以及教务系统名称。请勿提交账号、密码、带登录参数的链接或个人课表。

## 来源和许可证

- 应用代码基于 [拾光课程表](https://github.com/ShiGuangSchedule/shiguangschedule)，保留 [Apache 2.0 许可证](LICENSE)。
- 教务适配来自 [拾光教务适配仓库](https://github.com/ShiGuangSchedule/shiguang_warehouse)，保留 [MIT 许可证](shared/assets/offline_repo/LICENSE-shiguang-warehouse.txt) 和原脚本署名。
- [西安工业大学使用说明](西工课表-第一版.md)
