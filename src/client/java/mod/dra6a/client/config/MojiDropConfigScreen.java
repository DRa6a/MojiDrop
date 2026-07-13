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

	private final Screen lastScreen;

	private EditBox apiKeyBox;
	private EditBox apiUrlBox;
	private EditBox modelBox;
	private EditBox maxSuggestionsBox;
	private EditBox requestCooldownBox;
	private Button enabledButton;
	private boolean enabled;
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

		this.fieldWidth = Math.min(FIELD_WIDTH, this.width - MIN_SIDE_MARGIN * 2);
		if (this.fieldWidth < 160) {
			this.fieldWidth = 160;
		}
		int x = this.width / 2 - this.fieldWidth / 2;
		int fieldCount = 6;
		int requiredMinHeight = TOP_PADDING + fieldCount * BASE_SPACING + 10 + BUTTON_HEIGHT + BOTTOM_RESERVED;
		int spacing = BASE_SPACING;
		if (this.height < requiredMinHeight) {
			int available = this.height - TOP_PADDING - 10 - BUTTON_HEIGHT - BOTTOM_RESERVED;
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
		y += spacing + 10;

		Button saveButton = Button.builder(Component.literal("Save"), button -> this.saveConfig())
			.bounds(this.width / 2 - BUTTON_WIDTH - 5, y, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
		this.addRenderableWidget(saveButton);

		Button closeButton = Button.builder(Component.literal("Close"), button -> this.closeWithoutSaving())
			.bounds(this.width / 2 + 5, y, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
		this.addRenderableWidget(closeButton);
	}

	private Component enabledLabel() {
		return Component.literal("Enabled: " + this.enabled);
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
		this.drawLabel(graphics, "Max Suggestions（1-5）", this.maxSuggestionsBox.getY());
		this.drawLabel(graphics, "Request Cooldown（请求间隔，毫秒）", this.requestCooldownBox.getY());

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
