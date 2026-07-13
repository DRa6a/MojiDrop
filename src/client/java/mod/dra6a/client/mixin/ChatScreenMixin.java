package mod.dra6a.client.mixin;

import mod.dra6a.client.EmojiSuggestionDisplay;
import mod.dra6a.client.QaSuggestionDisplay;
import mod.dra6a.client.config.MojiDropConfig;
import mod.dra6a.client.service.EmojiSuggestionService;
import mod.dra6a.client.service.QaService;
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
	private static final String QA_TRIGGER = "#q:";

	@Unique
	private static final AtomicBoolean mojidrop$requestInFlight = new AtomicBoolean(false);

	@Unique
	private static volatile long mojidrop$lastRequestTime = 0L;

	@Unique
	private static volatile String mojidrop$lastRequestedText = null;

	@Unique
	private static final AtomicBoolean mojidrop$qaRequestInFlight = new AtomicBoolean(false);

	@Unique
	private static volatile long mojidrop$qaLastRequestTime = 0L;

	@Unique
	private static volatile String mojidrop$qaLastRequestedText = null;

	@Unique
	private static volatile int mojidrop$qaLastRangeStart = -1;

	@Unique
	private static volatile int mojidrop$qaLastRangeEnd = -1;

	@Unique
	private static volatile String mojidrop$qaLastTriggeredText = null;

	@Unique
	private static boolean mojidrop$apiConfigWarningShown = false;

	@Unique
	private static boolean mojidrop$requestErrorWarningShown = false;

	@Unique
	private static boolean mojidrop$qaConfigWarningShown = false;

	@Unique
	private static boolean mojidrop$qaRequestErrorWarningShown = false;

	@Shadow
	protected EditBox input;

	@Shadow
	private CommandSuggestions commandSuggestions;

	@Inject(method = "onEdited", at = @At("HEAD"))
	private void mojidrop$onChatEdited(String value, CallbackInfo ci) {
		if (value == null) {
			return;
		}

		MojiDropConfig config = MojiDropConfig.get();
		String trimmed = value.trim();
		boolean isCommand = trimmed.startsWith("/");

		if (config.qaEnabled && !isCommand && mojidrop$tryTriggerQa(value, config)) {
			return;
		}

		if (!config.enabled || isCommand) {
			return;
		}

		if (mojidrop$isUnansweredQaPresent(value)) {
			return;
		}

		if (!value.endsWith(" ")) {
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
	private boolean mojidrop$tryTriggerQa(String value, MojiDropConfig config) {
		if (mojidrop$qaLastTriggeredText != null && value.startsWith(mojidrop$qaLastTriggeredText)) {
			int nextQa = value.indexOf(QA_TRIGGER, mojidrop$qaLastTriggeredText.length());
			if (nextQa < 0) {
				return false;
			}
		}

		int triggerIndex = value.lastIndexOf(QA_TRIGGER);
		if (triggerIndex < 0) {
			return false;
		}

		int contentStart = triggerIndex + QA_TRIGGER.length();
		if (contentStart > value.length()) {
			return false;
		}

		String afterTrigger = value.substring(contentStart);
		String question;
		int questionEndInValue;

		if (config.qaStrictTrigger) {
			if (afterTrigger.startsWith(" ")) {
				return false;
			}

			int spaceIndex = afterTrigger.indexOf(' ');
			if (spaceIndex < 0) {
				return false;
			}

			question = afterTrigger.substring(0, spaceIndex).trim();
			questionEndInValue = contentStart + spaceIndex + 1;
		} else {
			if (!value.endsWith(" ")) {
				return false;
			}

			question = afterTrigger.trim();
			questionEndInValue = value.length();
		}

		if (question.isEmpty()) {
			return false;
		}

		long now = System.currentTimeMillis();
		if (now - mojidrop$qaLastRequestTime < config.requestCooldownMs) {
			MOJIDROP_LOGGER.debug("[MojiDrop] QA request skipped due to cooldown");
			return false;
		}

		if (mojidrop$qaRequestInFlight.get() && value.equals(mojidrop$qaLastRequestedText)) {
			return false;
		}

		mojidrop$qaLastRequestTime = now;
		mojidrop$qaLastRequestedText = value;
		mojidrop$qaRequestInFlight.set(true);
		mojidrop$qaLastTriggeredText = value.substring(0, questionEndInValue);

		boolean appendMode = "append".equals(config.qaAnswerMode);
		if (appendMode) {
			mojidrop$qaLastRangeStart = questionEndInValue;
			mojidrop$qaLastRangeEnd = questionEndInValue;
		} else {
			mojidrop$qaLastRangeStart = triggerIndex;
			mojidrop$qaLastRangeEnd = questionEndInValue;
		}

		Consumer<String> onSuccess = answer -> Minecraft.getInstance().execute(() -> {
			mojidrop$qaRequestInFlight.set(false);
			if (answer == null || answer.isEmpty()) {
				MOJIDROP_LOGGER.warn("[MojiDrop] Empty QA answer");
				return;
			}

			String currentValue = this.input.getValue();
			if (!currentValue.equals(mojidrop$qaLastRequestedText)) {
				MOJIDROP_LOGGER.debug("[MojiDrop] QA answer ignored because input changed");
				return;
			}

			String prefixed = QaService.makePrefixedAnswer(answer);
			String summary = QaService.makeSummary(prefixed);
			((QaSuggestionDisplay) this.commandSuggestions).mojidrop$showQaSuggestion(
				summary,
				prefixed,
				mojidrop$qaLastRangeStart,
				mojidrop$qaLastRangeEnd
			);
		});

		Consumer<Throwable> onError = error -> Minecraft.getInstance().execute(() -> {
			mojidrop$qaRequestInFlight.set(false);
			mojidrop$handleQaError(error);
		});

		QaService.requestAnswer(question, onSuccess, onError);
		return true;
	}

	@Unique
	private boolean mojidrop$isUnansweredQaPresent(String value) {
		if (!value.contains(QA_TRIGGER)) {
			return false;
		}

		int lastQa = value.lastIndexOf(QA_TRIGGER);
		String afterLastQa = value.substring(lastQa + QA_TRIGGER.length());
		String lower = afterLastQa.toLowerCase();
		return !lower.contains("a:") && !lower.contains("a：");
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
				if (displayMessage.length() > 240) {
					displayMessage = displayMessage.substring(0, 240);
				}
				Minecraft.getInstance().gui.getChat().addClientSystemMessage(
					Component.literal("[MojiDrop] 请求失败: " + displayMessage).withStyle(ChatFormatting.RED)
				);
			}
		}
	}

	@Unique
	private void mojidrop$handleQaError(Throwable error) {
		String message = error.getMessage();
		if (error instanceof IllegalStateException && "API key or URL not configured".equals(message)) {
			if (!mojidrop$qaConfigWarningShown) {
				mojidrop$qaConfigWarningShown = true;
				Minecraft.getInstance().gui.getChat().addClientSystemMessage(
					Component.literal("[MojiDrop 问答] API Key 或 API URL 未配置，请按 O 键打开配置界面").withStyle(ChatFormatting.RED)
				);
			}
		} else {
			MOJIDROP_LOGGER.error("[MojiDrop] QA request failed: {}", message, error);
			if (!mojidrop$qaRequestErrorWarningShown) {
				mojidrop$qaRequestErrorWarningShown = true;
				String displayMessage = message == null ? error.getClass().getSimpleName() : message;
				if (displayMessage.length() > 240) {
					displayMessage = displayMessage.substring(0, 240);
				}
				Minecraft.getInstance().gui.getChat().addClientSystemMessage(
					Component.literal("[MojiDrop 问答] 请求失败: " + displayMessage).withStyle(ChatFormatting.RED)
				);
			}
		}
	}
}
