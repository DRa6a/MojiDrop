package mod.dra6a.client.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mod.dra6a.client.config.MojiDropConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EmojiSuggestionService {
	private static final Logger LOGGER = LoggerFactory.getLogger("MojiDrop");
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private static final Pattern WORD_LIKE_PATTERN = Pattern.compile("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF]{3,}");

	public static void requestSuggestions(String context, Consumer<List<String>> onSuccess, Consumer<Throwable> onError) {
		MojiDropConfig config = MojiDropConfig.get();
		String apiKey = config.apiKey;
		String apiUrl = config.apiUrl;

		if (apiKey == null || apiKey.isBlank() || apiUrl == null || apiUrl.isBlank()) {
			onError.accept(new IllegalStateException("API key or URL not configured"));
			return;
		}

		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", config.model);
		requestBody.addProperty("max_tokens", 150);

		JsonArray messages = new JsonArray();

		JsonObject systemMessage = new JsonObject();
		systemMessage.addProperty("role", "system");
		String prompt = config.systemPrompt != null && !config.systemPrompt.isBlank()
			? config.systemPrompt
			: MojiDropConfig.DEFAULT_SYSTEM_PROMPT;
		systemMessage.addProperty("content", String.format(prompt, config.maxSuggestions));
		messages.add(systemMessage);

		JsonObject userMessage = new JsonObject();
		userMessage.addProperty("role", "user");
		userMessage.addProperty("content", context);
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

					String content = message.get("content").getAsString();
					List<String> rawSuggestions = Arrays.stream(content.split("[\n,]"))
						.map(String::trim)
						.filter(s -> !s.isEmpty())
						.collect(Collectors.toList());

					List<String> suggestions = rawSuggestions.stream()
						.filter(s -> !isWordLike(s))
						.limit(config.maxSuggestions)
						.collect(Collectors.toList());

					if (suggestions.isEmpty() && !rawSuggestions.isEmpty()) {
						LOGGER.warn("[MojiDrop] All {} emoji suggestions were filtered as word-like: {}", rawSuggestions.size(), rawSuggestions);
					}

					onSuccess.accept(suggestions);
				} catch (Exception e) {
					onError.accept(e);
				}
			})
			.exceptionally(throwable -> {
				onError.accept(throwable);
				return null;
			});
	}

	private static boolean isWordLike(String suggestion) {
		return WORD_LIKE_PATTERN.matcher(suggestion).find();
	}
}
