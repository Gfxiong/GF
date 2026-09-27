# 本次工程验证记录

日期：2026-09-27。环境：Linux、JDK 17、Gradle 8.11.1、Android SDK 35 / Build-Tools 35.0.0。

| 检查 | 实际结果 |
| --- | --- |
| `sh scripts/smoke-test.sh` | 通过：6 张支持范围图精确计数、2 张异常图提示、3 项输入检查 |
| 不等间距示例 | 输出 7；已检查 `build/uneven-result.png` 的 7 个框和编号 |
| 已知局限 | 接触图真实 2 / 输出 1；低对比图真实 3 / 输出 0；单独报告，不计为准确通过 |
| `:android-app:assembleDebug` | 通过，生成调试 APK |
| `:android-app:lintDebug` | 0 errors、6 warnings；涉及备份规则、启动图标和字符串国际化，未禁用检查 |
| Android 相机、选图、旋转、取消操作 | 尚未真机测试 |
| 工厂实拍准确率、手机耗时 | 尚未测试；缺少实拍图、人工标注和真机结果 |

合成图仅证明受限算法路径及测试流程能运行。构建成功仅证明可以产出 APK。
这两者均不能替代 `acceptance.md` 中的真机与实拍验收。

本地构建使用已核对官方 SHA-256 的 Gradle 8.11.1 发行包；仓库 Wrapper 同样固定版本和校验和。
GitHub Actions 后续结果以对应提交的 `Verify MVP` 运行记录为准。
