# OpenLinker Agent Notes

## 项目是什么

- 这是一个 IntelliJ IDEA 插件项目。
- 插件作用是按规则拼出链接，然后从菜单或工具栏打开。
- 设置入口在 `Settings > Tools > OpenLinker`。

## 当前已实现的能力

- `Tools > OpenLinker` 动作入口可用。
- 顶部工具栏有 `OpenLinker` 入口。
- 支持多条规则；每条规则包含名称、链接模板、启用状态。
- 支持给单条规则配置当前项目的 URL 覆盖；当前项目匹配时优先用项目规则。
- 支持变量：
  - `${PROJECT_NAME}`
  - `${MODULE_NAME}`
  - `${FILE_NAME}`
  - `${FILE_PATH}`
- 启用规则为 0 条时提示去设置页配置。
- 启用规则为 1 条时直接打开。
- 启用规则为多条时弹出选择。
- 普通地址走浏览器打开；`file://` 会在系统文件管理器里定位文件或目录。
- 配置会持久化保存。

## 关键位置

- 说明文档：`README.md`
- 插件声明：`src/main/resources/META-INF/plugin.xml`
- 打开逻辑入口：`src/main/kotlin/com/openlinker/OpenLinkerLauncher.kt`
- 设置保存：`src/main/kotlin/com/openlinker/settings/OpenLinkerSettingsService.kt`
- 设置界面：`src/main/kotlin/com/openlinker/settings/OpenLinkerSettingsPanel.kt`
- 规则判断：`src/main/kotlin/com/openlinker/url/OpenLinkerRuleDecider.kt`
- 模板替换：`src/main/kotlin/com/openlinker/url/OpenLinkerUrlTemplateResolver.kt`
- 测试目录：`src/test/kotlin/com/openlinker`

## 本地检查时要先注意

- 项目要求 Java 17。
- 开始开发或验收前，先确认终端里的 Java 已切到 17，再跑 `./gradlew test` 或 `./gradlew runIde`。

## 开发和验收约定

- 调整界面时，优先直接用 `./gradlew runIde` 在测试 IDE 里看效果；除非用户明确要安装包，否则不要默认打包。
- 规则设置页当前形态是：
  - 主页面用表格展示规则列表
  - 新增和编辑都走单独弹窗
- 编辑弹窗当前顺序和样式约定是：
  - 先 `Enabled`
  - 再 `Rule name`
  - 再 `URL template` 和变量按钮
  - 有项目名时显示项目覆盖勾选；勾选后显示项目 URL 输入框
  - 不显示 `Preview`
  - 底部说明左对齐
- 变量按钮需要看起来明显可点，不要做成普通文字链接的感觉。

## 持久化和安装

- 配置通过 `OpenLinkerSettings.xml` 持久化。
- 项目级覆盖与通用规则保存在同一份规则配置中。
- 这个插件当前要求安装或升级后重启 IDE，再继续验证配置保存和界面加载。

## 当前默认规则

- `GitHub -> https://www.github.com/your_username/${PROJECT_NAME}`
- `File -> file://${FILE_PATH}`

## 当前待办

- `TODO.md` 里记着一件后续事项：
  - 按项目和 IDE 级别分开设置规则
