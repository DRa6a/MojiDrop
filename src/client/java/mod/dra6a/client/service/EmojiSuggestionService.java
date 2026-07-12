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
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EmojiSuggestionService {
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final String SYSTEM_PROMPT = "You are a kaomoji suggestion assistant. Based on the chat context provided, suggest 1 to %d relevant kaomoji (Japanese emoticons). Return only the kaomoji, either one per line or separated by commas. Do not include explanations, labels, or markdown formatting.";

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
		systemMessage.addProperty("content", String.format(SYSTEM_PROMPT, config.maxSuggestions));
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
					List<String> suggestions = Arrays.stream(content.split("[\n,]"))
						.map(String::trim)
						.filter(s -> !s.isEmpty())
						.limit(config.maxSuggestions)
						.collect(Collectors.toList());

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
}
