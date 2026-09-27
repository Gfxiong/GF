# GF · 拍照计数 MVP

目标：在 Android 上拍一张照片，显示物品估计数量，并给每个检测到的物品画框、编号。
**第一步只验证单列、轮廓分离的物品；物品间距可以不同。** 先用塑料盖作为试验对象。

这是可运行的工程起点，不是已经达到工厂计数精度的成品。
目前只有合成图片测试，真实盖子准确率尚未测量。紧贴、套叠、透明、遮挡和杂乱背景可能误数，
即使没有警告也必须核对编号。

## 先做这一件事

用电脑安装 **JDK 17**，下载本分支，在项目根目录打开终端：

```sh
# macOS / Linux
sh scripts/smoke-test.sh
```

Windows：在项目根目录打开终端，运行 `scripts\smoke-test.bat`。
成功输出应包含 `PASS 8 image checks + 3 input checks`。
另外两行 `LIMITATION` 是故意展示的失败场景，不计入准确率。
这个步骤不需要 Android Studio、Python、模型、服务器或联网下载代码依赖。

再运行一张已知为 7 个、间距不等的示例：

```sh
java -Djava.awt.headless=true -cp build/smoke CountImage test-images/synthetic/uneven-7.png build/result.png
```

打开 `build/result.png`，应看到 7 个框及编号。
也可替换为自己的图片路径（路径含空格要加双引号）。电脑入口推荐使用方向已转正的 JPG / PNG；
手机入口会处理照片方向。先手工裁去无关区域，并保留物品四周的背景。

## 在 Android 上运行

1. 安装 [Android Studio](https://developer.android.com/studio)，选择 **Open**，打开本项目根目录。
2. 在 SDK Manager 安装 **Android SDK Platform 35** 和 **Build-Tools 35.0.0**；
   Gradle JDK 选择 **JDK 17**。第一次同步需要联网。
3. 连接 Android 9 或更新的手机，开启开发者选项和 USB 调试，允许电脑调试。
4. 选择 `android-app`，点击 Run。先点“运行 7 个物品示例”，再点“拍照计数”或“选择照片”。
5. 对照实际物品逐个检查框和编号，记录误数的图片，不要只看总数。

命令行构建（已配置 `ANDROID_HOME`，或让 Android Studio 生成 `local.properties`）：

```sh
./gradlew :android-app:assembleDebug
# Windows: gradlew.bat :android-app:assembleDebug
```

APK 位于 `android-app/build/outputs/apk/debug/android-app-debug.apk`。
GitHub 的 `Verify MVP` 工作流也执行测试及构建；成功后可在该次运行的 Artifacts 下载 `gf-debug-apk`。
调试包用于试验，未配置商店发布或正式签名。

## 首版包含什么

| 部分 | 实现与边界 |
| --- | --- |
| Android 端 | 单页；系统相机拍照 / 选择照片 / 内置示例；本地计数及编号图；取消与读取失败提示 |
| 计数算法 | 浅色背景上的深色连通区域计数；不使用固定间距、总高度除以单件高度等估算 |
| 共用代码 | `counter-core/` 的纯 Java 算法同时供 Android 和电脑测试调用 |
| 测试图片 | `test-images/` 的 10 张合成图、真实数量标签和真实照片采集规范 |
| 验收 | `docs/acceptance.md` 区分代码检查、真机流程、真实照片精度 |

照片只在手机本地处理；应用没有联网权限，不上传照片。
调用系统相机写入应用缓存，不读取整个相册，也不需要申请相机或存储运行时权限。
首版选择 Java + 系统界面，是为了让计数逻辑能直接用 JDK 测试，并避免两套算法产生差异。

## 文件从哪里看起

| 路径 | 用途 |
| --- | --- |
| `counter-core/src/main/java/com/gfxiong/gf/core/Counter.java` | 输入像素，返回物品框和异常提示 |
| `android-app/src/main/java/com/gfxiong/gf/MainActivity.java` | 拍照、读取图片、后台计数、显示结果 |
| `tools/CountImage.java` | 在电脑上测试自己的图片并输出编号图 |
| `tools/CoreSmokeTest.java` | 自动核对已知样例 |
| `tools/GenerateFixtures.java` | 重新生成同一套合成图：`java tools/GenerateFixtures.java` |
| `docs/algorithm.md` | 算法步骤、参数、失败原因 |
| `docs/acceptance.md` | 验收标准与下一步 |

**接下来只做一项：** 收集同一种盖子的 10 张实拍图，逐张人工标注准确数量，
先检查哪些照片符合“独立轮廓”假设。如果工作现场必须计数紧贴或套叠盖子，
先用这些照片评估分界线/目标检测方案，再扩充应用。首版不增加账号、云服务、训练系统、批量相册或库存功能。

工具版本固定为 AGP 8.9.2、Gradle 8.11.1、JDK 17、compile/target SDK 35。
依据：[AGP 兼容表](https://developer.android.com/build/releases/agp-8-9-0-release-notes)、
[TakePicture](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.TakePicture)、
[FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider)。
