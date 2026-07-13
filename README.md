# MojiDrop

MojiDrop 是一个 Minecraft Fabric 客户端模组，能够在玩家输入聊天信息时，根据当前语境实时提供 1–5 个颜文字（kaomoji）建议，类似编程工具中的 AI 代码补全。

## 功能

- **AI 实时补全**：在聊天输入框中输入空格后，模组会自动向配置的 AI 接口请求颜文字建议。
- **@ 玩家提及**：在聊天输入框中输入 `@` 后，会自动弹出在线玩家选择列表，支持方向键选择、TAB 或鼠标点击插入玩家名称。
- **AI 问答**：输入 `#q:问题 `（严格模式下问题与冒号之间无空格，且问题内不能有空格，第一个空格即表示问题结束）后，模组会向 AI 请求简短回答，并在建议列表显示约 10 字的答案概况，按 TAB 或点击即可将完整答案（带 `a:` 前缀）填入聊天框。
- **不打断命令输入**：输入 `/` 开头的命令时不会触发 AI 请求，避免干扰命令补全。
- **原版建议列表**：返回的颜文字与玩家提及建议均以 Minecraft 原版的命令提示列表形式展示，支持方向键选择、TAB 或鼠标点击插入。
- **可配置**：
  - API Key
  - API 请求地址（OpenAI 兼容格式）
  - 模型名称
  - 系统提示词（System Prompt）
  - 最大建议数量（1–5）
  - 请求冷却时间（毫秒）
  - 功能总开关
- **快捷键**：按 `O` 键打开 MojiDrop 配置界面。
- **异常处理**：
  - API Key 或地址未配置时跳过请求并提示配置。
  - 请求过快时静默忽略。
  - 网络或 API 失败时显示一次性聊天栏提示。

## 环境要求

- Minecraft 26.1.2
- Fabric Loader 0.19.2+
- Fabric API 0.154.2+
- Java 25

## 使用方法

1. 将 `mojidrop-<version>.jar` 放入 `.minecraft/mods` 文件夹。
2. 启动游戏后，按 `O` 键打开配置界面，填入 API Key 和请求地址。
3. 进入聊天输入框，输入任意内容后再按一次空格，等待 AI 返回颜文字建议。
4. 输入 `@` 可触发在线玩家选择列表，方便快速 @ 其他玩家。
5. 输入 `#q:问题 `（严格模式下问题与冒号之间无空格，问题内不能有空格，第一个空格表示问题结束）可向 AI 提问，返回后在建议栏看到答案概况，按 `TAB` 填充完整答案。
   - 替换模式：用 `a: 答案` 替换掉 `#q:问题 `。
   - 追加模式：在空格后追加 `a: 答案`，保留你输入的内容。
6. 使用方向键选择建议，按 `TAB` 或鼠标点击将建议内容插入到当前输入文本中。

## 配置项

| 配置项 | 说明 | 默认值 |
| --- | --- | --- |
| API Key | AI 服务的 API Key | 空 |
| API URL | OpenAI 兼容的聊天补全接口地址 | `https://api.openai.com/v1/chat/completions` |
| Model | 使用的模型名称 | `gpt-3.5-turbo` |
| System Prompt | 发送给 AI 的系统提示词，控制返回的颜文字风格 | 见下 |
| Max Suggestions | 每次请求最多返回的颜文字数量（1–5） | `3` |
| Request Cooldown | 两次请求之间的最小间隔（毫秒） | `500` |
| Enabled | 是否启用 AI 补全 | `true` |
| QA Enabled | 是否启用 `#q:` 问答功能 | `true` |
| QA Use Same AI | 问答是否使用与颜文字相同的 AI 配置 | `true` |
| QA API Key / URL / Model | 问答单独使用的 AI 配置（QA Use Same AI 为 false 时生效） | 空 |
| QA System Prompt | 问答 AI 的系统提示词 | 见下 |
| QA Max Tokens | 限制问答回答的最大长度 | `150` |
| QA Answer Mode | 回答填充模式：`replace` 替换问题，`append` 追加回答 | `replace` |
| QA Strict Trigger | 严格触发格式：问题与冒号之间、问题内均不能有空格 | `true` |

默认 System Prompt：

```
You are a kaomoji suggestion assistant. Based on the chat context provided, suggest 1 to %d relevant kaomoji (Japanese emoticons made of Unicode characters, NOT colon-style codes like :thinking:). Output ONLY the kaomoji themselves, either one per line or separated by commas. Never output colon-style emoticon codes, markdown, explanations, labels, or any other text.
```

其中 `%d` 会被替换为“最大建议数量”，不建议在自定义提示词中删除它。

默认 QA System Prompt：

```
You are a concise Q&A assistant. Answer the user's question briefly and clearly. Keep answers short. Do not use Markdown formatting because the game chat does not support it. Provide only the answer itself. Do not add follow-up offers such as 'if you have more questions' or similar closing sentences.
```

建议保留“简短回答”和“不添加结束语”相关描述，避免 AI 返回过长内容或多余句子。

## 配置文件

配置文件位于：

```
.minecraft/config/mojidrop/config.json
```

你可以直接编辑该文件来批量修改配置，修改后下次进入游戏生效。

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/mojidrop-<version>.jar`。

## 许可证

本项目基于 CC0 许可证发布。
