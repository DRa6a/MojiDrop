package mod.dra6a.client.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mod.dra6a.client.config.MojiDropConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class QaService {
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

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

		if (apiKey == null || apiKey.isBlank() || apiUrl == null || apiUrl.isBlank()) {
			onError.accept(new IllegalStateException("API key or URL not configured"));
			return;
		}

		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", model);
		requestBody.addProperty("max_tokens", config.qaMaxTokens);

		JsonArray messages = new JsonArray();

		JsonObject systemMessage = new JsonObject();
		systemMessage.addProperty("role", "system");
		String prompt = config.qaSystemPrompt != null && !config.qaSystemPrompt.isBlank()
			? config.qaSystemPrompt
			: MojiDropConfig.DEFAULT_QA_SYSTEM_PROMPT;
		systemMessage.addProperty("content", prompt);
		messages.add(systemMessage);

		JsonObject userMessage = new JsonObject();
		userMessage.addProperty("role", "user");
		userMessage.addProperty("content", question);
		messages.add(userMessage);

		requestBody.add("messages", messages);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl))
			.header("Content-Type", "application/json")
			.header("Authorization", "Bearer " + apiKey)
			.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
			.build();

		HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
			.thenAccept(response -> {
				try {
					if (response.statusCode() < 200 || response.statusCode() >= 300) {
						throw new RuntimeException("Unexpected HTTP status: " + response.statusCode() + " body: " + response.body());
					}

					JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
					JsonArray choices = jsonResponse.getAsJsonArray("choices");
					if (choices == null || choices.isEmpty()) {
						throw new RuntimeException("No choices in response");
					}

					JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
					if (message == null) {
						throw new RuntimeException("No message in first choice");
					}

					String content = sanitizeAnswer(message.get("content").getAsString());
					if (content.isEmpty()) {
						throw new RuntimeException("Empty answer");
					}

					onSuccess.accept(content);
				} catch (Exception e) {
					onError.accept(e);
				}
			})
			.exceptionally(throwable -> {
				onError.accept(throwable);
				return null;
			});
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
