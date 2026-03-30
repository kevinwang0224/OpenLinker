# OpenLinker

OpenLinker 是一个 IntelliJ IDEA 插件工程示例。它提供一个 `OpenLinker` 按钮，并在 `Settings > Tools > OpenLinker` 里管理多条 URL 规则。

## 功能

- 提供一个 `OpenLinker` Action，入口在 `Tools` 菜单
- 在顶部工具栏放一个 `OpenLinker` 图标，点击后会在图标旁边弹出一个小菜单
- 支持多条规则，每条规则包含：
  - `name`
  - `urlTemplate`
  - `enabled`
- 支持变量：
  - `${PROJECT_NAME}`
  - `${MODULE_NAME}`
  - `${FILE_NAME}`
  - `${FILE_PATH}`
- 执行逻辑：
  - 0 条启用规则：提示去设置页配置
  - 1 条启用规则：直接打开
  - 多条启用规则：弹窗选择后打开
- 普通地址使用 `BrowserUtil.browse()` 打开浏览器
- `file://` 地址会在系统文件管理器里定位到文件或目录
- 使用 `PersistentStateComponent` 持久化配置
- 设置页左边规则栏带一个快速打开按钮，选中后可以直接点开
- 支持把规则导出成 JSON 文件，也支持从 JSON 文件追加导入，方便团队共享
- 默认内置规则：
  - `Google -> https://www.google.com/search?q=${PROJECT_NAME}`
  - `File -> file://${FILE_PATH}`

## 环境要求

- Java 17
- IntelliJ IDEA 2023.1 或更高版本

## 运行

1. 用 IntelliJ IDEA 打开工程。
2. 确保 Gradle JVM 使用 Java 17。
3. 运行 `runIde` 任务启动测试用 IDE。

如果你更习惯命令行，也可以直接运行：

```bash
./gradlew runIde
```

## 使用方式

1. 打开 `Settings > Tools > OpenLinker`
2. 添加、修改、导入或导出规则
3. 在菜单里点击 `Tools > OpenLinker`

### 导入导出

- 导出入口在设置页工具栏，支持导出全部规则，或只导出当前选中的规则
- 导入入口也在设置页工具栏，导入内容会追加到当前列表末尾
- 导入后的变更和普通编辑一样，要点 `Apply` 或 `OK` 才会真正保存
- 导入导出文件使用 JSON，结构如下：

```json
{
  "version": 1,
  "rules": [
    {
      "name": "Google",
      "urlTemplate": "https://www.google.com/search?q=${PROJECT_NAME}",
      "enabled": true
    }
  ]
}
```

## 变量说明

- `PROJECT_NAME`：当前项目名
- `MODULE_NAME`：当前模块名；拿不到时为空
- `FILE_NAME`：当前文件名；拿不到时为空
- `FILE_PATH`：当前文件完整路径；拿不到时为空

## 测试

工程里带了基础测试，覆盖这些核心情况：

- 默认规则是否存在
- 规则保存后的读取结果
- 规则导入导出的 JSON 格式和校验
- URL 模板变量替换
- 0 条 / 1 条 / 多条启用规则的分支判断

可运行：

```bash
./gradlew test
```
