# MojiDrop AI 颜文字补全 Spec

## Why
玩家在原版 Minecraft 聊天输入时，经常需要手动输入颜文字来表达情绪。MojiDrop 应当在玩家输入聊天内容时，根据当前语境实时提供 1-5 个颜文字建议，并通过原版的命令提示/TAB 补全机制快速插入，从而提升聊天表达效率。

## What Changes
- 新增配置系统：支持在游戏内配置 API key、请求地址、模型、最大建议数、请求冷却时间等。
- 新增配置界面：使用 Minecraft 原版 Screen 实现一个轻量级配置界面，并通过客户端快捷键（如 `O` 键）在任意界面打开。
- 新增聊天输入监听：通过 Mixin 或 Fabric 客户端事件监听聊天框文本变化，当用户输入空格时触发 AI 建议请求。
- 新增 AI 请求服务：使用 Java `HttpClient` 异步向配置的 API 地址发送请求，携带当前聊天上下文，返回 1-5 个颜文字候选。
- 新增颜文字建议展示：将 AI 返回的颜文字注入到原版 `ChatInputSuggestor` 建议列表，支持方向键选择和 TAB 补全插入。
- 新增异常与频率控制：处理网络连接失败、API 返回错误、请求过快、空结果、解析失败等情况，避免崩溃并给出用户反馈。
- 更新构建产物：构建可工作的 jar 包，并将变更推送到远程仓库。

## Impact
- Affected specs: 无既有 spec。
- Affected code:
  - `src/client/java/mod/dra6a/client/` 下新增配置、网络、建议、GUI 相关类。
  - `src/main/java/mod/dra6a/MojiDrop.java` 可能用于注册配置保存。
  - `src/client/resources/mojidrop.client.mixins.json` 可能新增聊天框 Mixin。
  - `build.gradle` 与 `gradle.properties` 保持简洁，不引入外部 mod 依赖。
  - `fabric.mod.json` 更新描述、作者、依赖信息。

## ADDED Requirements

### Requirement: 配置系统
The system SHALL 提供一个持久化的客户端配置系统，允许玩家在游戏内配置以下字段：
- API key（字符串，敏感项）
- API 请求地址（字符串，默认指向一个常见的 OpenAI 兼容端点）
- 模型名称（字符串）
- 每次返回的最大颜文字数量（整数，范围 1-5，默认 3）
- 请求冷却时间（整数毫秒，默认 500ms）
- 功能总开关（布尔值，默认开启）

#### Scenario: 配置保存与加载
- **WHEN** 玩家打开配置界面并修改字段后保存
- **THEN** 配置在客户端持久化，并在下次启动游戏时恢复

### Requirement: 配置界面
The system SHALL 提供一个使用 Minecraft 原版 Screen 实现的配置界面，并通过客户端快捷键打开。

#### Scenario: 打开配置
- **WHEN** 玩家在游戏中按下配置快捷键（默认 `O` 键）
- **THEN** 弹出配置界面，显示所有可配置项

### Requirement: 聊天输入监听与触发
The system SHALL 监听原版聊天输入框（`ChatScreen`）的文本变化，当玩家输入空格且当前功能开启时，触发 AI 颜文字建议请求。

#### Scenario: 输入空格触发请求
- **GIVEN** 玩家正在聊天框中输入文本
- **WHEN** 玩家输入空格且距离上一次请求超过冷却时间
- **THEN** 系统异步向 AI 发送请求，携带当前聊天文本

#### Scenario: 请求去重与冷却
- **GIVEN** 玩家连续快速输入多个空格
- **WHEN** 下一次空格触发时间距离上一次不足冷却时间
- **THEN** 系统忽略本次触发，不发送新请求

### Requirement: AI 请求服务
The system SHALL 使用 Java 内置 HTTP 客户端异步发送请求，并解析返回的颜文字列表。

#### Scenario: 成功返回颜文字
- **WHEN** AI 服务返回 1-5 个颜文字
- **THEN** 系统将候选颜文字传递给聊天建议器展示

#### Scenario: 网络或 API 失败
- **WHEN** 网络不可达或 API 返回非 2xx 状态码
- **THEN** 系统记录日志，向玩家显示一次性聊天提示（不阻塞输入），且不展示建议

### Requirement: 原版建议与 TAB 补全
The system SHALL 将颜文字候选注入到原版聊天建议列表，允许玩家使用方向键选择并按 TAB 插入。

#### Scenario: 展示与选择建议
- **WHEN** AI 返回颜文字候选
- **THEN** 聊天框下方显示建议列表
- **AND** 玩家按 TAB 或点击建议时，将颜文字追加到当前输入末尾

### Requirement: 异常处理
The system SHALL 对以下异常情况提供保护：
- 网络连接失败：显示用户友好的提示，不崩溃。
- 请求过快：通过冷却时间限制请求频率。
- API 返回空结果或解析失败：不展示建议，可选记录日志。
- 配置缺失：当 API key 或请求地址为空时，直接跳过请求并提示玩家前往配置。

## MODIFIED Requirements
无。

## REMOVED requirements
无。
