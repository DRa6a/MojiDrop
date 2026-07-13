package mod.dra6a.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MojiDropConfigScreen extends Screen {
	private static final int FIELD_WIDTH = 320;
	private static final int FIELD_HEIGHT = 20;
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BASE_SPACING = 34;
	private static final int TOP_PADDING = 35;
	private static final int BOTTOM_RESERVED = 35;
	private static final int MIN_SIDE_MARGIN = 20;
	private static final int SECTION_EXTRA_SPACING = 8;

	private final Screen lastScreen;

	private EditBox apiKeyBox;
	private EditBox apiUrlBox;
	private EditBox modelBox;
	private EditBox systemPromptBox;
	private EditBox maxSuggestionsBox;
	private EditBox requestCooldownBox;
	private Button enabledButton;
	private boolean enabled;

	private Button qaEnabledButton;
	private Button qaUseSameAiButton;
	private EditBox qaApiKeyBox;
	private EditBox qaApiUrlBox;
	private EditBox qaModelBox;
	private EditBox qaSystemPromptBox;
	private EditBox qaMaxTokensBox;
	private boolean qaEnabled;
	private boolean qaUseSameAi;

	private Component statusMessage;
	private int statusMessageTicks;
	private int fieldWidth;

	public MojiDropConfigScreen(Screen lastScreen) {
		super(Component.literal("MojiDrop Config"));
		this.lastScreen = lastScreen;
	}

	@Override
	protected void init() {
		MojiDropConfig config = MojiDropConfig.get();
		this.enabled = config.enabled;
		this.qaEnabled = config.qaEnabled;
		this.qaUseSameAi = config.qaUseSameAi;

		this.fieldWidth = Math.min(FIELD_WIDTH, this.width - MIN_SIDE_MARGIN * 2);
		if (this.fieldWidth < 160) {
			this.fieldWidth = 160;
		}
		int x = this.width / 2 - this.fieldWidth / 2;
		int fieldCount = 13;
		int requiredMinHeight = TOP_PADDING + fieldCount * BASE_SPACING + SECTION_EXTRA_SPACING + 10 + BUTTON_HEIGHT + BOTTOM_RESERVED;
		int spacing = BASE_SPACING;
		if (this.height < requiredMinHeight) {
			int available = this.height - TOP_PADDING - SECTION_EXTRA_SPACING - 10 - BUTTON_HEIGHT - BOTTOM_RESERVED;
			if (available > fieldCount * 22) {
				spacing = available / fieldCount;
			} else {
				spacing = 22;
			}
		}

		int y = TOP_PADDING;

		this.apiKeyBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("API Key"));
		this.apiKeyBox.setMaxLength(8192);
		this.apiKeyBox.setValue(config.apiKey);
		this.apiKeyBox.setHint(Component.literal("例如：sk-...").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.addRenderableWidget(this.apiKeyBox);
		y += spacing;

		this.apiUrlBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("API URL"));
		this.apiUrlBox.setMaxLength(4096);
		this.apiUrlBox.setValue(config.apiUrl);
		this.apiUrlBox.setHint(Component.literal("OpenAI 兼容地址").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.addRenderableWidget(this.apiUrlBox);
		y += spacing;

		this.modelBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("Model"));
		this.modelBox.setMaxLength(512);
		this.modelBox.setValue(config.model);
		this.modelBox.setHint(Component.literal("例如：gpt-3.5-turbo").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.addRenderableWidget(this.modelBox);
		y += spacing;

		this.systemPromptBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("System Prompt"));
		this.systemPromptBox.setMaxLength(8192);
		this.systemPromptBox.setValue(config.systemPrompt);
		this.systemPromptBox.setHint(Component.literal("控制 AI 返回颜文字的风格").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.addRenderableWidget(this.systemPromptBox);
		y += spacing;

		this.maxSuggestionsBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("Max Suggestions"));
		this.maxSuggestionsBox.setMaxLength(1);
		this.maxSuggestionsBox.setValue(String.valueOf(config.maxSuggestions));
		this.maxSuggestionsBox.setHint(Component.literal("1 - 5").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.allowOnlyDigits(this.maxSuggestionsBox);
		this.addRenderableWidget(this.maxSuggestionsBox);
		y += spacing;

		this.requestCooldownBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("Request Cooldown (ms)"));
		this.requestCooldownBox.setMaxLength(9);
		this.requestCooldownBox.setValue(String.valueOf(config.requestCooldownMs));
		this.requestCooldownBox.setHint(Component.literal("两次请求间隔，毫秒").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.allowOnlyDigits(this.requestCooldownBox);
		this.addRenderableWidget(this.requestCooldownBox);
		y += spacing;

		this.enabledButton = Button.builder(this.enabledLabel(), button -> {
			this.enabled = !this.enabled;
			button.setMessage(this.enabledLabel());
		}).bounds(x, y, this.fieldWidth, FIELD_HEIGHT).build();
		this.addRenderableWidget(this.enabledButton);
		y += spacing + SECTION_EXTRA_SPACING;

		this.qaEnabledButton = Button.builder(this.qaEnabledLabel(), button -> {
			this.qaEnabled = !this.qaEnabled;
			button.setMessage(this.qaEnabledLabel());
		}).bounds(x, y, this.fieldWidth, FIELD_HEIGHT).build();
		this.addRenderableWidget(this.qaEnabledButton);
		y += spacing;

		this.qaUseSameAiButton = Button.builder(this.qaUseSameAiLabel(), button -> {
			this.qaUseSameAi = !this.qaUseSameAi;
			button.setMessage(this.qaUseSameAiLabel());
			this.updateQaApiFieldsEditable();
		}).bounds(x, y, this.fieldWidth, FIELD_HEIGHT).build();
		this.addRenderableWidget(this.qaUseSameAiButton);
		y += spacing;

		this.qaApiKeyBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("QA API Key"));
		this.qaApiKeyBox.setMaxLength(8192);
		this.qaApiKeyBox.setValue(config.qaApiKey);
		this.qaApiKeyBox.setHint(Component.literal("留空则使用上方 API Key").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.addRenderableWidget(this.qaApiKeyBox);
		y += spacing;

		this.qaApiUrlBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("QA API URL"));
		this.qaApiUrlBox.setMaxLength(4096);
		this.qaApiUrlBox.setValue(config.qaApiUrl);
		this.qaApiUrlBox.setHint(Component.literal("留空则使用上方 API URL").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.addRenderableWidget(this.qaApiUrlBox);
		y += spacing;

		this.qaModelBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("QA Model"));
		this.qaModelBox.setMaxLength(512);
		this.qaModelBox.setValue(config.qaModel);
		this.qaModelBox.setHint(Component.literal("留空则使用上方 Model").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.addRenderableWidget(this.qaModelBox);
		y += spacing;

		this.qaSystemPromptBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("QA System Prompt"));
		this.qaSystemPromptBox.setMaxLength(8192);
		this.qaSystemPromptBox.setValue(config.qaSystemPrompt);
		this.qaSystemPromptBox.setHint(Component.literal("控制问答 AI 的回答风格").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.addRenderableWidget(this.qaSystemPromptBox);
		y += spacing;

		this.qaMaxTokensBox = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal("QA Max Tokens"));
		this.qaMaxTokensBox.setMaxLength(4);
		this.qaMaxTokensBox.setValue(String.valueOf(config.qaMaxTokens));
		this.qaMaxTokensBox.setHint(Component.literal("限制回答长度").withStyle(net.minecraft.ChatFormatting.GRAY));
		this.allowOnlyDigits(this.qaMaxTokensBox);
		this.addRenderableWidget(this.qaMaxTokensBox);
		y += spacing + 10;

		Button saveButton = Button.builder(Component.literal("Save"), button -> this.saveConfig())
			.bounds(this.width / 2 - BUTTON_WIDTH - 5, y, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
		this.addRenderableWidget(saveButton);

		Button closeButton = Button.builder(Component.literal("Close"), button -> this.closeWithoutSaving())
			.bounds(this.width / 2 + 5, y, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
		this.addRenderableWidget(closeButton);

		this.updateQaApiFieldsEditable();
	}

	private Component enabledLabel() {
		return Component.literal("Emoji Enabled: " + this.enabled);
	}

	private Component qaEnabledLabel() {
		return Component.literal("QA Enabled: " + this.qaEnabled);
	}

	private Component qaUseSameAiLabel() {
		return Component.literal("QA Use Same AI: " + this.qaUseSameAi);
	}

	private void updateQaApiFieldsEditable() {
		boolean editable = !this.qaUseSameAi;
		this.qaApiKeyBox.setEditable(editable);
		this.qaApiUrlBox.setEditable(editable);
		this.qaModelBox.setEditable(editable);
	}

	private void allowOnlyDigits(EditBox box) {
		box.setResponder(value -> {
			if (!value.matches("\\d*")) {
				box.setValue(value.replaceAll("[^\\d]", ""));
			}
		});
	}

	private void saveConfig() {
		MojiDropConfig config = MojiDropConfig.get();
		config.apiKey = this.apiKeyBox.getValue();
		config.apiUrl = this.apiUrlBox.getValue();
		config.model = this.modelBox.getValue();
		config.systemPrompt = this.systemPromptBox.getValue();

		int maxSuggestions;
		try {
			maxSuggestions = Integer.parseInt(this.maxSuggestionsBox.getValue());
		} catch (NumberFormatException e) {
			maxSuggestions = 3;
		}
		if (maxSuggestions < 1) {
			maxSuggestions = 1;
		} else if (maxSuggestions > 5) {
			maxSuggestions = 5;
		}
		config.maxSuggestions = maxSuggestions;

		int cooldown;
		try {
			cooldown = Integer.parseInt(this.requestCooldownBox.getValue());
		} catch (NumberFormatException e) {
			cooldown = 500;
		}
		if (cooldown < 0) {
			cooldown = 0;
		}
		config.requestCooldownMs = cooldown;

		config.enabled = this.enabled;

		config.qaEnabled = this.qaEnabled;
		config.qaUseSameAi = this.qaUseSameAi;
		config.qaApiKey = this.qaApiKeyBox.getValue();
		config.qaApiUrl = this.qaApiUrlBox.getValue();
		config.qaModel = this.qaModelBox.getValue();
		config.qaSystemPrompt = this.qaSystemPromptBox.getValue();

		int qaMaxTokens;
		try {
			qaMaxTokens = Integer.parseInt(this.qaMaxTokensBox.getValue());
		} catch (NumberFormatException e) {
			qaMaxTokens = 150;
		}
		if (qaMaxTokens < 1) {
			qaMaxTokens = 1;
		} else if (qaMaxTokens > 2048) {
			qaMaxTokens = 2048;
		}
		config.qaMaxTokens = qaMaxTokens;

		MojiDropConfig.save();
		this.statusMessage = Component.literal("配置已保存").withStyle(net.minecraft.ChatFormatting.GREEN);
		this.statusMessageTicks = 60;
	}

	private void closeWithoutSaving() {
		this.minecraft.setScreen(this.lastScreen);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.lastScreen);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.statusMessageTicks > 0) {
			this.statusMessageTicks--;
			if (this.statusMessageTicks <= 0) {
				this.statusMessage = null;
			}
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		int titleWidth = this.font.width(this.title);
		graphics.text(this.font, this.title, this.width / 2 - titleWidth / 2, 20, 0xFFFFFF);

		this.drawLabel(graphics, "API Key（从 AI 服务商获取）", this.apiKeyBox.getY());
		this.drawLabel(graphics, "API URL（OpenAI 兼容接口地址）", this.apiUrlBox.getY());
		this.drawLabel(graphics, "Model（模型名称）", this.modelBox.getY());
		this.drawLabel(graphics, "System Prompt（系统提示词，%d 表示最大数量）", this.systemPromptBox.getY());
		this.drawLabel(graphics, "Max Suggestions（1-5）", this.maxSuggestionsBox.getY());
		this.drawLabel(graphics, "Request Cooldown（请求间隔，毫秒）", this.requestCooldownBox.getY());
		this.drawLabel(graphics, "问答功能（#q: 触发）", this.qaEnabledButton.getY());
		this.drawLabel(graphics, "QA System Prompt（控制问答风格，要求简短回答）", this.qaSystemPromptBox.getY());
		this.drawLabel(graphics, "QA Max Tokens（限制回答长度）", this.qaMaxTokensBox.getY());

		if (this.statusMessage != null) {
			int statusWidth = this.font.width(this.statusMessage);
			graphics.text(this.font, this.statusMessage, this.width / 2 - statusWidth / 2, this.height - 20, 0xFFFFFF);
		}
	}

	private void drawLabel(GuiGraphicsExtractor graphics, String text, int fieldY) {
		Component label = Component.literal(text);
		int x = this.width / 2 - this.fieldWidth / 2;
		int y = fieldY - 11;
		graphics.text(this.font, label, x, y, 0xAAAAAA);
	}
}
