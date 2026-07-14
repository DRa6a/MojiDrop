package mod.dra6a.client.service;

import mod.dra6a.client.config.MojiDropConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EmojiSuggestionService {
	public static void requestSuggestions(String context, Consumer<List<String>> onSuccess, Consumer<Throwable> onError) {
		MojiDropConfig config = MojiDropConfig.get();

		String prompt = config.systemPrompt != null && !config.systemPrompt.isBlank()
			? config.systemPrompt
			: MojiDropConfig.DEFAULT_SYSTEM_PROMPT;
		String systemPrompt = String.format(prompt, config.maxSuggestions);

		AiRequestService.request(
			config.apiKey,
			config.apiUrl,
			config.model,
			systemPrompt,
			context,
			150,
			config.apiMode,
			content -> {
				DebugLogService.log("Emoji", context, content);

				List<String> suggestions = Arrays.stream(content.split("[\n,]"))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.limit(config.maxSuggestions)
					.collect(Collectors.toList());

				onSuccess.accept(suggestions);
			},
			onError
		);
	}
}
