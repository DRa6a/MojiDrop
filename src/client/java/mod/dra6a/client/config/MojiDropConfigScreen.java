package mod.dra6a.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MojiDropConfigScreen extends Screen {
	private static final int FIELD_WIDTH = 320;
	private static final int FIELD_HEIGHT = 20;
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BASE_SPACING = 34;
	private static final int TOP_PADDING = 45;
	private static final int BOTTOM_RESERVED = 45;
	private static final int MIN_SIDE_MARGIN = 20;
	private static final int SECTION_EXTRA_SPACING = 10;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int SCROLLBAR_MARGIN = 4;

	private final Screen lastScreen;

	private EditBox apiKeyBox;
	private EditBox apiUrlBox;
	private EditBox modelBox;
	private EditBox systemPromptBox;
	private EditBox maxSuggestionsBox;
	private EditBox requestCooldownBox;
	private Button enabledButton;
	private Button debugLoggingButton;
	private Button apiModeButton;
	private boolean enabled;
	private boolean debugLogging;
	private String apiMode;

	private Button qaEnabledButton;
	private Button qaUseSameAiButton;
	private Button qaAnswerModeButton;
	private Button qaStrictTriggerButton;
	private EditBox qaApiKeyBox;
	private EditBox qaApiUrlBox;
	private EditBox qaModelBox;
	private EditBox qaSystemPromptBox;
	private EditBox qaMaxTokensBox;
	private boolean qaEnabled;
	private boolean qaUseSameAi;
	private String qaAnswerMode;
	private boolean qaStrictTrigger;

	private Component statusMessage;
	private int statusMessageTicks;
	private int fieldWidth;

	private int scrollOffset = 0;
	private int contentHeight = 0;
	private int viewportTop;
	private int viewportBottom;
	private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
	private final List<LabelEntry> scrollableLabels = new ArrayList<>();

	public MojiDropConfigScreen(Screen lastScreen) {
		super(Component.literal("MojiDrop 配置"));
		this.lastScreen = lastScreen;
	}

	@Override
	protected void init() {
		this.scrollableWidgets.clear();
		this.scrollableLabels.clear();

		MojiDropConfig config = MojiDropConfig.get();
		this.enabled = config.enabled;
		this.debugLogging = config.debugLogging;
		this.apiMode = config.apiMode;
		this.qaEnabled = config.qaEnabled;
		this.qaUseSameAi = config.qaUseSameAi;
		this.qaAnswerMode = config.qaAnswerMode;
		this.qaStrictTrigger = config.qaStrictTrigger;

		this.fieldWidth = Math.min(FIELD_WIDTH, this.width - MIN_SIDE_MARGIN * 2 - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN);
		if (this.fieldWidth < 160) {
			this.fieldWidth = 160;
		}

		this.viewportTop = TOP_PADDING;
		this.viewportBottom = this.height - BOTTOM_RESERVED;

		int x = this.width / 2 - this.fieldWidth / 2 - (SCROLLBAR_WIDTH + SCROLLBAR_MARGIN) / 2;
		int y = 0;

		this.addSectionLabel("颜文字补全", y);
		y += 18;

		this.apiModeButton = Button.builder(this.apiModeLabel(), btn -> {
			this.apiMode = switch (this.apiMode) {
				case "mixed" -> "api";
				case "api" -> "fallback";
				default -> "mixed";
			};
			btn.setMessage(this.apiModeLabel());
		}).bounds(x, y, this.fieldWidth, FIELD_HEIGHT).build();
		this.addScrollableWidget(this.apiModeButton);
		y += BASE_SPACING;

		this.apiKeyBox = this.addEditBox(x, y, "API 密钥", config.apiKey, "例如：sk-...", 8192);
		y += BASE_SPACING;

		this.apiUrlBox = this.addEditBox(x, y, "API 地址", config.apiUrl, "OpenAI 兼容地址", 4096);
		y += BASE_SPACING;

		this.modelBox = this.addEditBox(x, y, "模型", config.model, "例如：gpt-3.5-turbo", 512);
		y += BASE_SPACING;

		this.systemPromptBox = this.addEditBox(x, y, "系统提示词", config.systemPrompt, "%d 表示最大数量", 8192);
		y += BASE_SPACING;

		this.maxSuggestionsBox = this.addDigitEditBox(x, y, "最大建议数", String.valueOf(config.maxSuggestions), "1 - 5", 1);
		y += BASE_SPACING;

		this.requestCooldownBox = this.addDigitEditBox(x, y, "请求间隔（毫秒）", String.valueOf(config.requestCooldownMs), "两次请求间隔", 9);
		y += BASE_SPACING;

		this.enabledButton = this.addToggleButton(x, y, this::enabledLabel, () -> this.enabled = !this.enabled);
		y += BASE_SPACING;

		this.debugLoggingButton = this.addToggleButton(x, y, this::debugLoggingLabel, () -> this.debugLogging = !this.debugLogging);
		y += BASE_SPACING + SECTION_EXTRA_SPACING;

		this.addSectionLabel("AI 问答（#q: 触发）", y);
		y += 18;

		this.qaEnabledButton = this.addToggleButton(x, y, this::qaEnabledLabel, () -> this.qaEnabled = !this.qaEnabled);
		y += BASE_SPACING;

		this.qaUseSameAiButton = this.addToggleButton(x, y, this::qaUseSameAiLabel, () -> {
			this.qaUseSameAi = !this.qaUseSameAi;
			this.updateQaApiFieldsEditable();
		});
		y += BASE_SPACING;

		this.qaAnswerModeButton = Button.builder(this.qaAnswerModeLabel(), btn -> {
			this.qaAnswerMode = "replace".equals(this.qaAnswerMode) ? "append" : "replace";
			btn.setMessage(this.qaAnswerModeLabel());
		}).bounds(x, y, this.fieldWidth, FIELD_HEIGHT).build();
		this.addScrollableWidget(this.qaAnswerModeButton);
		y += BASE_SPACING;

		this.qaStrictTriggerButton = this.addToggleButton(x, y, this::qaStrictTriggerLabel, () -> this.qaStrictTrigger = !this.qaStrictTrigger);
		y += BASE_SPACING;

		this.qaApiKeyBox = this.addEditBox(x, y, "问答 API 密钥", config.qaApiKey, "与上方相同时留空", 8192);
		y += BASE_SPACING;

		this.qaApiUrlBox = this.addEditBox(x, y, "问答 API 地址", config.qaApiUrl, "与上方相同时留空", 4096);
		y += BASE_SPACING;

		this.qaModelBox = this.addEditBox(x, y, "问答 模型", config.qaModel, "与上方相同时留空", 512);
		y += BASE_SPACING;

		this.qaSystemPromptBox = this.addEditBox(x, y, "问答系统提示词", config.qaSystemPrompt, "控制问答风格，建议要求简短", 8192);
		y += BASE_SPACING;

		this.qaMaxTokensBox = this.addDigitEditBox(x, y, "问答最大长度（Tokens）", String.valueOf(config.qaMaxTokens), "限制回答长度", 4);
		y += BASE_SPACING + 10;

		this.contentHeight = y;
		this.clampScrollOffset();
		this.applyScrollOffset();
		this.updateQaApiFieldsEditable();

		int saveY = this.height - BOTTOM_RESERVED + 10;
		Button saveButton = Button.builder(Component.literal("保存"), button -> this.saveConfig())
			.bounds(this.width / 2 - BUTTON_WIDTH - 5, saveY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
		this.addRenderableWidget(saveButton);

		Button closeButton = Button.builder(Component.literal("关闭"), button -> this.closeWithoutSaving())
			.bounds(this.width / 2 + 5, saveY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
		this.addRenderableWidget(closeButton);
	}

	private EditBox addEditBox(int x, int y, String label, String value, String hint, int maxLength) {
		EditBox box = new EditBox(this.font, x, y, this.fieldWidth, FIELD_HEIGHT, Component.literal(label));
		box.setMaxLength(maxLength);
		box.setValue(value);
		box.setHint(Component.literal(hint).withStyle(net.minecraft.ChatFormatting.GRAY));
		this.addScrollableWidget(box);
		this.addLabel(label, y);
		return box;
	}

	private EditBox addDigitEditBox(int x, int y, String label, String value, String hint, int maxLength) {
		EditBox box = this.addEditBox(x, y, label, value, hint, maxLength);
		this.allowOnlyDigits(box);
		return box;
	}

	private Button addToggleButton(int x, int y, java.util.function.Supplier<Component> messageSupplier, Runnable toggleAction) {
		Button button = Button.builder(messageSupplier.get(), btn -> {
			toggleAction.run();
			btn.setMessage(messageSupplier.get());
		}).bounds(x, y, this.fieldWidth, FIELD_HEIGHT).build();
		this.addScrollableWidget(button);
		return button;
	}

	private void addScrollableWidget(AbstractWidget widget) {
		this.scrollableWidgets.add(widget);
		this.addRenderableWidget(widget);
	}

	private void addLabel(String text, int y) {
		this.scrollableLabels.add(new LabelEntry(text, y));
	}

	private void addSectionLabel(String text, int y) {
		this.scrollableLabels.add(new LabelEntry(text, y, 0xFFFFFF, true));
	}

	private Component enabledLabel() {
		return Component.literal("启用颜文字补全：" + (this.enabled ? "是" : "否"));
	}

	private Component debugLoggingLabel() {
		return Component.literal("启用调试日志：" + (this.debugLogging ? "是" : "否"));
	}

	private Component apiModeLabel() {
		String label = switch (this.apiMode) {
			case "api" -> "仅使用用户 API";
			case "fallback" -> "仅使用兜底 API";
			default -> "混用模式（用户 API 优先）";
		};
		return Component.literal("API 模式：" + label);
	}

	private Component qaEnabledLabel() {
		return Component.literal("启用 AI 问答：" + (this.qaEnabled ? "是" : "否"));
	}

	private Component qaUseSameAiLabel() {
		return Component.literal("问答使用相同 AI：" + (this.qaUseSameAi ? "是" : "否"));
	}

	private Component qaAnswerModeLabel() {
		String mode = "replace".equals(this.qaAnswerMode) ? "替换问题" : "追加回答";
		return Component.literal("问答回答模式：" + mode);
	}

	private Component qaStrictTriggerLabel() {
		return Component.literal("严格触发格式：" + (this.qaStrictTrigger ? "开" : "关"));
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

	private void clampScrollOffset() {
		int viewportHeight = this.viewportBottom - this.viewportTop;
		int maxScroll = Math.max(0, this.contentHeight - viewportHeight);
		if (this.scrollOffset < 0) {
			this.scrollOffset = 0;
		} else if (this.scrollOffset > maxScroll) {
			this.scrollOffset = maxScroll;
		}
	}

	private void applyScrollOffset() {
		for (AbstractWidget widget : this.scrollableWidgets) {
			widget.setY(widget.getY() - this.scrollOffset);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX >= 0 && mouseX <= this.width && mouseY >= this.viewportTop && mouseY <= this.viewportBottom) {
			int oldOffset = this.scrollOffset;
			this.scrollOffset -= (int) (scrollY * 20);
			this.clampScrollOffset();
			int delta = oldOffset - this.scrollOffset;
			if (delta != 0) {
				for (AbstractWidget widget : this.scrollableWidgets) {
					widget.setY(widget.getY() + delta);
				}
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
		config.debugLogging = this.debugLogging;
		config.apiMode = this.apiMode;

		config.qaEnabled = this.qaEnabled;
		config.qaUseSameAi = this.qaUseSameAi;
		config.qaApiKey = this.qaApiKeyBox.getValue();
		config.qaApiUrl = this.qaApiUrlBox.getValue();
		config.qaModel = this.qaModelBox.getValue();
		config.qaSystemPrompt = this.qaSystemPromptBox.getValue();
		config.qaAnswerMode = this.qaAnswerMode;
		config.qaStrictTrigger = this.qaStrictTrigger;

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
		this.minecraft.setScreenAndShow(this.lastScreen);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.lastScreen);
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

		graphics.enableScissor(0, this.viewportTop, this.width, this.viewportBottom);
		for (LabelEntry label : this.scrollableLabels) {
			int y = label.baseY - this.scrollOffset;
			if (y + 10 >= this.viewportTop && y <= this.viewportBottom) {
				Component component = Component.literal(label.text);
				int x = this.width / 2 - this.fieldWidth / 2 - (SCROLLBAR_WIDTH + SCROLLBAR_MARGIN) / 2;
				graphics.text(this.font, component, x, y, label.color);
			}
		}
		graphics.disableScissor();

		this.drawScrollbar(graphics);

		if (this.statusMessage != null) {
			int statusWidth = this.font.width(this.statusMessage);
			graphics.text(this.font, this.statusMessage, this.width / 2 - statusWidth / 2, this.height - 20, 0xFFFFFF);
		}
	}

	private void drawScrollbar(GuiGraphicsExtractor graphics) {
		int viewportHeight = this.viewportBottom - this.viewportTop;
		if (this.contentHeight <= viewportHeight) {
			return;
		}

		int trackHeight = viewportHeight;
		int thumbHeight = Math.max(20, (int) ((double) viewportHeight / this.contentHeight * trackHeight));
		int maxScroll = this.contentHeight - viewportHeight;
		int thumbY = this.viewportTop + (int) ((double) this.scrollOffset / maxScroll * (trackHeight - thumbHeight));
		int scrollbarX = this.width / 2 + this.fieldWidth / 2 + SCROLLBAR_MARGIN;

		graphics.fill(scrollbarX, this.viewportTop, scrollbarX + SCROLLBAR_WIDTH, this.viewportBottom, 0x33000000);
		graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFFAAAAAA);
	}

	private static class LabelEntry {
		final String text;
		final int baseY;
		final int color;
		final boolean section;

		LabelEntry(String text, int baseY) {
			this(text, baseY, 0xAAAAAA, false);
		}

		LabelEntry(String text, int baseY, int color, boolean section) {
			this.text = text;
			this.baseY = baseY;
			this.color = color;
			this.section = section;
		}
	}
}
