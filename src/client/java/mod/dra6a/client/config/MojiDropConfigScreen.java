package mod.dra6a.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MojiDropConfigScreen extends Screen {
	private static final int FIELD_WIDTH = 200;
	private static final int FIELD_HEIGHT = 20;
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 20;

	private final Screen lastScreen;

	private EditBox apiKeyBox;
	private EditBox apiUrlBox;
	private EditBox modelBox;
	private EditBox maxSuggestionsBox;
	private EditBox requestCooldownBox;
	private Button enabledButton;
	private boolean enabled;

	public MojiDropConfigScreen(Screen lastScreen) {
		super(Component.literal("MojiDrop Config"));
		this.lastScreen = lastScreen;
	}

	@Override
	protected void init() {
		MojiDropConfig config = MojiDropConfig.get();
		this.enabled = config.enabled;

		int x = this.width / 2 - FIELD_WIDTH / 2;
		int y = 50;
		int spacing = 30;

		this.apiKeyBox = new EditBox(this.font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("API Key"));
		this.apiKeyBox.setValue(config.apiKey);
		this.apiKeyBox.setMaxLength(256);
		this.addRenderableWidget(this.apiKeyBox);
		y += spacing;

		this.apiUrlBox = new EditBox(this.font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("API URL"));
		this.apiUrlBox.setValue(config.apiUrl);
		this.apiUrlBox.setMaxLength(256);
		this.addRenderableWidget(this.apiUrlBox);
		y += spacing;

		this.modelBox = new EditBox(this.font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Model"));
		this.modelBox.setValue(config.model);
		this.modelBox.setMaxLength(128);
		this.addRenderableWidget(this.modelBox);
		y += spacing;

		this.maxSuggestionsBox = new EditBox(this.font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Max Suggestions"));
		this.maxSuggestionsBox.setValue(String.valueOf(config.maxSuggestions));
		this.maxSuggestionsBox.setMaxLength(1);
		this.allowOnlyDigits(this.maxSuggestionsBox);
		this.addRenderableWidget(this.maxSuggestionsBox);
		y += spacing;

		this.requestCooldownBox = new EditBox(this.font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Request Cooldown (ms)"));
		this.requestCooldownBox.setValue(String.valueOf(config.requestCooldownMs));
		this.requestCooldownBox.setMaxLength(9);
		this.allowOnlyDigits(this.requestCooldownBox);
		this.addRenderableWidget(this.requestCooldownBox);
		y += spacing;

		this.enabledButton = Button.builder(this.enabledLabel(), button -> {
			this.enabled = !this.enabled;
			button.setMessage(this.enabledLabel());
		}).bounds(x, y, FIELD_WIDTH, FIELD_HEIGHT).build();
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
		this.closeWithoutSaving();
	}

	private void closeWithoutSaving() {
		this.minecraft.setScreen(this.lastScreen);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.lastScreen);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		int titleWidth = this.font.width(this.title);
		graphics.text(this.font, this.title, this.width / 2 - titleWidth / 2, 20, 0xFFFFFF);

		this.drawLabel(graphics, "API Key", this.apiKeyBox.getY());
		this.drawLabel(graphics, "API URL", this.apiUrlBox.getY());
		this.drawLabel(graphics, "Model", this.modelBox.getY());
		this.drawLabel(graphics, "Max Suggestions", this.maxSuggestionsBox.getY());
		this.drawLabel(graphics, "Request Cooldown (ms)", this.requestCooldownBox.getY());
		this.drawLabel(graphics, "Enabled", this.enabledButton.getY());
	}

	private void drawLabel(GuiGraphicsExtractor graphics, String text, int fieldY) {
		Component label = Component.literal(text);
		int labelWidth = this.font.width(label);
		int x = this.width / 2 - FIELD_WIDTH / 2 - labelWidth - 8;
		int y = fieldY + (FIELD_HEIGHT - 9) / 2;
		graphics.text(this.font, label, x, y, 0xFFFFFF);
	}
}
