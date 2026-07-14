package mod.dra6a.client.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class AiRequestService {
	public static final String FALLBACK_URL = "https://text.pollinations.ai";
	public static final String FALLBACK_MODEL = "openai";

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	public static void request(
		String apiKey,
		String apiUrl,
		String model,
		String systemPrompt,
		String userContent,
		int maxTokens,
		String mode,
		Consumer<String> onSuccess,
		Consumer<Throwable> onError
	) {
		boolean fallbackOnly = "fallback".equals(mode);
		boolean apiOnly = "api".equals(mode);
		boolean userApiConfigured = apiKey != null && !apiKey.isBlank()
			&& apiUrl != null && !apiUrl.isBlank();

		if (fallbackOnly) {
			sendFallback(systemPrompt, userContent, onSuccess, onError);
			return;
		}

		if (apiOnly) {
			if (!userApiConfigured) {
				onError.accept(new IllegalStateException("API key or URL not configured"));
				return;
			}
			sendUserApi(apiKey, apiUrl, model, systemPrompt, userContent, maxTokens, onSuccess, onError);
			return;
		}

		// 混用模式：优先用户 API，不可用时使用兜底
		if (!userApiConfigured) {
			sendFallback(systemPrompt, userContent, onSuccess, onError);
			return;
		}

		sendUserApi(apiKey, apiUrl, model, systemPrompt, userContent, maxTokens,
			onSuccess,
			throwable -> sendFallback(systemPrompt, userContent, onSuccess, onError)
		);
	}

	private static void sendUserApi(
		String apiKey,
		String apiUrl,
		String model,
		String systemPrompt,
		String userContent,
		int maxTokens,
		Consumer<String> onSuccess,
		Consumer<Throwable> onError
	) {
		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", model);
		requestBody.addProperty("max_tokens", maxTokens);
		requestBody.addProperty("reasoning_effort", "low");

		JsonArray messages = new JsonArray();

		JsonObject systemMessage = new JsonObject();
		systemMessage.addProperty("role", "system");
		systemMessage.addProperty("content", systemPrompt);
		messages.add(systemMessage);

		JsonObject userMessage = new JsonObject();
		userMessage.addProperty("role", "user");
		userMessage.addProperty("content", userContent);
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

					onSuccess.accept(message.get("content").getAsString());
				} catch (Exception e) {
					onError.accept(e);
				}
			})
			.exceptionally(throwable -> {
				onError.accept(throwable);
				return null;
			});
	}

	private static void sendFallback(
		String systemPrompt,
		String userContent,
		Consumer<String> onSuccess,
		Consumer<Throwable> onError
	) {
		try {
			String encodedPrompt = encodeUrl(userContent);
			StringBuilder url = new StringBuilder(FALLBACK_URL)
				.append('/')
				.append(encodedPrompt)
				.append("?model=")
				.append(encodeUrl(FALLBACK_MODEL));

			if (systemPrompt != null && !systemPrompt.isBlank()) {
				url.append("&system=").append(encodeUrl(systemPrompt));
			}

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url.toString()))
				.header("Content-Type", "application/json")
				.GET()
				.build();

			HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenAccept(response -> {
					try {
						if (response.statusCode() < 200 || response.statusCode() >= 300) {
							throw new RuntimeException("Fallback HTTP status: " + response.statusCode() + " body: " + response.body());
						}

						onSuccess.accept(response.body());
					} catch (Exception e) {
						onError.accept(e);
					}
				})
				.exceptionally(throwable -> {
					onError.accept(throwable);
					return null;
				});
		} catch (Exception e) {
			onError.accept(e);
		}
	}

	private static String encodeUrl(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
