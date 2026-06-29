# OpenLinker

OpenLinker 是一个 JetBrains IDE 插件，可基于可配置的 URL 模板，打开与当前上下文相关的网页链接或本地文件链接。

### 插件功能

- 可从顶部工具栏图标或 `Tools > OpenLinker` 打开链接
- 支持通过可配置规则和占位变量（项目、模块、文件上下文）拼接 URL
- 支持为单条规则配置项目级 URL 覆盖
- 支持设置全局浏览器，也支持为单条规则单独指定浏览器
- 同时支持 **网页链接** 与 `file://` 路径（会在系统文件管理器中定位）
- 可在 `Settings > Tools > OpenLinker` 管理、持久化、导入和导出规则

### 安装方式

当前仓库以源码工程形式提供。体验插件可按以下步骤：

1. 用 IntelliJ IDEA 打开项目
2. 运行 `./gradlew runIde`
3. 在沙箱 IDE 中直接使用 OpenLinker

### 快速开始

1. 打开 `Settings > Tools > OpenLinker`
2. 添加、编辑、导入或导出规则
3. 点击 `Tools > OpenLinker`（或使用顶部工具栏图标）

### 截图

工具栏弹出菜单：

![OpenLinker toolbar popup](docs/images/openlinker-toolbar-menu.png)

设置页（规则列表）：

![OpenLinker settings rules](docs/images/openlinker-settings-rules.png)

### 导入与导出

- 导出入口位于设置页工具栏，可导出全部规则或仅导出当前选中规则
- 导入入口也在设置页工具栏，导入后会追加到当前规则列表末尾
- 导入后的改动与普通编辑一致，需点击 `Apply` 或 `OK` 才会持久化
- 导入/导出文件使用 JSON，结构如下：

```json
{
  "version": 1,
  "rules": [
    {
      "name": "GitHub(Example)",
      "urlTemplate": "https://github.com/your_username/${PROJECT_NAME}",
      "enabled": false,
      "projectOverrides": [
        {
          "projectName": "OpenLinker",
          "urlTemplate": "https://github.com/openlinker/${PROJECT_NAME}"
        }
      ]
    }
  ]
}
```

当规则中存在与当前项目名匹配的 `projectOverrides` 时，OpenLinker 会优先使用项目级 URL 模板；否则继续使用规则的默认 `urlTemplate`。

浏览器选择属于个人设置，不会包含在导入/导出文件里。

### 变量说明

- `PROJECT_NAME`：当前项目名
- `MODULE_NAME`：当前模块名；不可用时为空
- `FILE_NAME`：当前文件名；不可用时为空
- `FILE_PATH`：当前文件完整路径；不可用时为空

### 环境要求

- Java 17
- IntelliJ IDEA 2023.1 或更高版本

### 本地运行

1. 用 IntelliJ IDEA 打开项目。
2. 确保 Gradle JVM 使用 Java 17。
3. 运行 `runIde` 任务启动沙箱 IDE。

如果你更习惯命令行，也可以运行：

```bash
./gradlew runIde
```

### 测试

项目包含基础测试，覆盖以下核心场景：

- 默认规则是否存在
- 配置持久化后的规则读取结果
- 规则导入/导出的 JSON 格式与校验
- 项目级规则覆盖
- URL 模板变量替换
- 0 / 1 / 多条启用规则的分支决策

运行：

```bash
./gradlew test
```
