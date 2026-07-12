package mod.dra6a.client.mixin;

import mod.dra6a.client.EmojiSuggestionDisplay;
import mod.dra6a.client.config.MojiDropConfig;
import mod.dra6a.client.service.EmojiSuggestionService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	private static final Logger MOJIDROP_LOGGER = LoggerFactory.getLogger("MojiDrop");

	@Unique
	private static final AtomicBoolean mojidrop$requestInFlight = new AtomicBoolean(false);

	@Unique
	private static volatile long mojidrop$lastRequestTime = 0L;

	@Unique
	private static volatile String mojidrop$lastRequestedText = null;

	@Unique
	private static boolean mojidrop$apiConfigWarningShown = false;

	@Unique
	private static boolean mojidrop$requestErrorWarningShown = false;

	@Shadow
	protected EditBox input;

	@Shadow
	private CommandSuggestions commandSuggestions;

	@Inject(method = "onEdited", at = @At("HEAD"))
	private void mojidrop$onChatEdited(String value, CallbackInfo ci) {
		MojiDropConfig config = MojiDropConfig.get();
		if (!config.enabled) {
			return;
		}

		if (value == null || !value.endsWith(" ")) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - mojidrop$lastRequestTime < config.requestCooldownMs) {
			MOJIDROP_LOGGER.debug("[MojiDrop] Request skipped due to cooldown");
			return;
		}

		if (mojidrop$requestInFlight.get() && value.equals(mojidrop$lastRequestedText)) {
			return;
		}

		mojidrop$lastRequestTime = now;
		mojidrop$lastRequestedText = value;
		mojidrop$requestInFlight.set(true);

		Consumer<List<String>> onSuccess = suggestions -> Minecraft.getInstance().execute(() -> {
			mojidrop$requestInFlight.set(false);
			if (suggestions == null || suggestions.isEmpty()) {
				MOJIDROP_LOGGER.warn("[MojiDrop] Empty emoji suggestion result");
				return;
			}
			((EmojiSuggestionDisplay) this.commandSuggestions).mojidrop$showEmojiSuggestions(suggestions);
		});

		Consumer<Throwable> onError = error -> Minecraft.getInstance().execute(() -> {
			mojidrop$requestInFlight.set(false);
			mojidrop$handleRequestError(error);
		});

		EmojiSuggestionService.requestSuggestions(value, onSuccess, onError);
	}

	@Unique
	private void mojidrop$handleRequestError(Throwable error) {
		String message = error.getMessage();
		if (error instanceof IllegalStateException && "API key or URL not configured".equals(message)) {
			if (!mojidrop$apiConfigWarningShown) {
				mojidrop$apiConfigWarningShown = true;
				Minecraft.getInstance().gui.getChat().addClientSystemMessage(
					Component.literal("[MojiDrop] API Key 或 API URL 未配置，请按 O 键打开配置界面").withStyle(ChatFormatting.RED)
				);
			}
		} else {
			MOJIDROP_LOGGER.error("[MojiDrop] Emoji suggestion request failed: {}", message, error);
			if (!mojidrop$requestErrorWarningShown) {
				mojidrop$requestErrorWarningShown = true;
				String displayMessage = message == null ? error.getClass().getSimpleName() : message;
				if (displayMessage.length() > 80) {
					displayMessage = displayMessage.substring(0, 80);
				}
				Minecraft.getInstance().gui.getChat().addClientSystemMessage(
					Component.literal("[MojiDrop] 请求失败: " + displayMessage).withStyle(ChatFormatting.RED)
				);
			}
		}
	}
}
