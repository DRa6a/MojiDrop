# MojiDrop

MojiDrop 是一个 Minecraft Fabric 客户端模组，能够在玩家输入聊天信息时，根据当前语境实时提供 1–5 个颜文字（kaomoji）建议，类似编程工具中的 AI 代码补全。

## 功能

- **AI 实时补全**：在聊天输入框中输入空格后，模组会自动向配置的 AI 接口请求颜文字建议。
- **原版建议列表**：返回的颜文字以 Minecraft 原版的命令提示列表形式展示，支持方向键选择、TAB 或鼠标点击插入。
- **可配置**：
  - API Key
  - API 请求地址（OpenAI 兼容格式）
  - 模型名称
  - 最大建议数量（1–5）
  - 请求冷却时间（毫秒）
  - 功能总开关
- **快捷键**：按 `O` 键打开 MojiDrop 配置界面。
- **异常处理**：
  - API Key 或地址未配置时跳过请求并提示配置。
  - 请求过快时静默忽略。
  - 网络或 API 失败时显示一次性聊天栏提示。

## 环境要求

- Minecraft 1.21.5（26.1.2）
- Fabric Loader 0.19.3+
- Fabric API 0.154.2+
- Java 25

## 使用方法

1. 将 `mojidrop-<version>.jar` 放入 `.minecraft/mods` 文件夹。
2. 启动游戏后，按 `O` 键打开配置界面，填入 API Key 和请求地址。
3. 进入聊天输入框，输入任意内容后再按一次空格，等待 AI 返回颜文字建议。
4. 使用方向键选择建议，按 `TAB` 或鼠标点击将颜文字追加到当前输入文本末尾。

## 配置项

| 配置项 | 说明 | 默认值 |
| --- | --- | --- |
| API Key | AI 服务的 API Key | 空 |
| API URL | OpenAI 兼容的聊天补全接口地址 | `https://api.openai.com/v1/chat/completions` |
| Model | 使用的模型名称 | `gpt-3.5-turbo` |
| Max Suggestions | 每次请求最多返回的颜文字数量（1–5） | `3` |
| Request Cooldown | 两次请求之间的最小间隔（毫秒） | `500` |
| Enabled | 是否启用 AI 补全 | `true` |

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/mojidrop-<version>.jar`。

## 许可证

本项目基于 CC0 许可证发布。
