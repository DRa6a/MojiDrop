# Tasks

- [x] Task 1: 添加配置系统与 Cloth Config / ModMenu 依赖
  - [x] SubTask 1.1: 在 `build.gradle` 与 `gradle.properties` 中添加 Cloth Config API 与 ModMenu 依赖。
  - [x] SubTask 1.2: 创建 `MojiDropConfig` 配置数据类，包含 API key、请求地址、模型、最大建议数、冷却时间、总开关字段。
  - [x] SubTask 1.3: 实现配置的保存与加载（使用 `FabricLoader.getInstance().getConfigDir()`）。
  - [x] SubTask 1.4: 更新 `fabric.mod.json` 中的依赖、描述与作者信息。

- [x] Task 2: 实现配置界面（使用原版 Screen，不依赖 Cloth Config / ModMenu）
  - [x] SubTask 2.1: 创建 `MojiDropConfigScreen.java`，使用 Minecraft 原版 Screen 与 EditBox/Button 渲染配置字段。
  - [x] SubTask 2.2: 注册客户端快捷键（如 `O` 键）打开配置界面。
  - [x] SubTask 2.3: 验证配置界面可保存并加载配置。

- [x] Task 3: 实现聊天输入监听与触发逻辑
  - [x] SubTask 3.1: 通过 Mixin 注入 `ChatScreen` 或相关类，监听文本变化。
  - [x] SubTask 3.2: 当检测到空格输入且满足冷却时间与功能开关条件时，触发 AI 请求。
  - [x] SubTask 3.3: 在请求未返回前，避免重复触发相同文本的请求。

- [x] Task 4: 实现 AI 请求服务
  - [x] SubTask 4.1: 创建 `EmojiSuggestionService`，使用 `HttpClient` 异步发送请求。
  - [x] SubTask 4.2: 构造符合 OpenAI 兼容格式的请求体，包含当前聊天上下文与请求数量。
  - [x] SubTask 4.3: 解析响应，提取 1-5 个颜文字候选。
  - [x] SubTask 4.4: 处理网络错误、非 2xx 响应、JSON 解析错误等异常情况。

- [x] Task 5: 实现颜文字建议与 TAB 补全
  - [x] SubTask 5.1: 将 AI 返回的颜文字候选转换为原版 `Suggestion` 对象。
  - [x] SubTask 5.2: 通过 Mixin 或 API 将建议注入到 `ChatInputSuggestor`。
  - [x] SubTask 5.3: 实现 TAB 或鼠标选择后，将颜文字追加到聊天输入框当前文本。
  - [x] SubTask 5.4: 验证方向键选择与高亮显示正常。

- [x] Task 6: 实现异常处理与用户提示
  - [x] SubTask 6.1: 当 API key 或请求地址为空时，跳过请求并向玩家显示配置提示。
  - [x] SubTask 6.2: 当请求过快时，静默忽略并在调试日志中记录。
  - [x] SubTask 6.3: 当网络或 API 失败时，向玩家显示一次性聊天栏提示（如红色文字）。
  - [x] SubTask 6.4: 当返回空结果或解析失败时，不展示建议，记录日志。

- [ ] Task 7: 构建产物并推送
  - [ ] SubTask 7.1: 运行 `./gradlew build` 生成 jar 包。
  - [ ] SubTask 7.2: 确认 `build/libs/mojidrop-1.0.0.jar` 存在且可加载。
  - [ ] SubTask 7.3: 提交所有代码与 spec 文档，并推送到远程仓库。

# Task Dependencies
- Task 2 依赖 Task 1
- Task 3 依赖 Task 1
- Task 4 依赖 Task 1
- Task 5 依赖 Task 3 与 Task 4
- Task 6 依赖 Task 3、Task 4 与 Task 5
- Task 7 依赖 Task 1 至 Task 6
