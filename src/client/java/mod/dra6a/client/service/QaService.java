package mod.dra6a.client.service;

import mod.dra6a.client.config.MojiDropConfig;

import java.util.List;
import java.util.function.Consumer;

public class QaService {
	private static final List<String> TRAILING_OFFER_PATTERNS = List.of(
		"有什么可以帮你的吗[？?]",
		"有什么我能帮你的吗[？?]",
		"有什么我可以帮你的吗[？?]",
		"有什么需要我帮忙的吗[？?]",
		"还有什么可以帮你的吗[？?]",
		"还需要我帮你什么吗[？?]",
		"还有其他问题吗[？?]",
		"如果[^。]*(?:请|随时|告诉我|联系我)[^。]*[。]?",
		"If you have[^.]*questions[^.]*[.]?",
		"If you need[^.]*help[^.]*[.]?",
		"Feel free to ask[^.]*[.]?",
		"Let me know if[^.]*[.]?",
		"Do you have any other questions[^.]*[.]?"
	);

	public static void requestAnswer(String question, Consumer<String> onSuccess, Consumer<Throwable> onError) {
		MojiDropConfig config = MojiDropConfig.get();

		String apiKey;
		String apiUrl;
		String model;
		if (config.qaUseSameAi) {
			apiKey = config.apiKey;
			apiUrl = config.apiUrl;
			model = config.model;
		} else {
			apiKey = config.qaApiKey;
			apiUrl = config.qaApiUrl;
			model = config.qaModel;
		}

		String prompt = config.qaSystemPrompt != null && !config.qaSystemPrompt.isBlank()
			? config.qaSystemPrompt
			: MojiDropConfig.DEFAULT_QA_SYSTEM_PROMPT;

		AiRequestService.request(
			apiKey,
			apiUrl,
			model,
			prompt,
			question,
			config.qaMaxTokens,
			config.apiMode,
			rawContent -> {
				DebugLogService.log("QA", question, rawContent);
				String content = sanitizeAnswer(rawContent);
				if (content.isEmpty()) {
					onError.accept(new RuntimeException("Empty answer"));
					return;
				}
				onSuccess.accept(content);
			},
			onError
		);
	}

	public static String sanitizeAnswer(String answer) {
		String result = answer.trim();
		for (String pattern : TRAILING_OFFER_PATTERNS) {
			if (result.isEmpty()) {
				break;
			}
			result = result.replaceFirst("(?i)[\\s。]*" + pattern + "\\s*$", "").trim();
		}
		return result;
	}

	public static String makeSummary(String answer) {
		String stripped = answer.replaceFirst("^[Aa][:]\\s*", "").trim();
		if (stripped.isEmpty()) {
			return answer;
		}

		int end = Math.min(stripped.length(), 10);
		String summary = stripped.substring(0, end);
		if (stripped.length() > 10) {
			summary += "…";
		}
		return summary;
	}

	public static String makePrefixedAnswer(String answer) {
		String trimmed = answer.trim();
		if (trimmed.toLowerCase().startsWith("a:")) {
			return trimmed;
		}
		return "a: " + trimmed;
	}
}
