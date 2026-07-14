package mod.dra6a.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class MojiDropConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mojidrop/config.json");

	private static MojiDropConfig INSTANCE;

	public static final String DEFAULT_SYSTEM_PROMPT = "You are a kaomoji suggestion assistant. Kaomoji are text-based emoticons such as (^_^), ( ・ω・)ノ, ヾ(•ω•`)o, (╯°□°）╯︵ ┻━┻, (´；ω；｀). They are NOT Japanese words like こんにちは, バイバイ, やあ, and NOT colon-style codes like :thinking:. Based on the chat context provided, suggest 1 to %d relevant kaomoji. Output ONLY the kaomoji themselves, either one per line or separated by commas. Never output Japanese words, Chinese words, colon-style codes, markdown, explanations, labels, or any other text.";
	public static final String DEFAULT_QA_SYSTEM_PROMPT = "You are a concise Q&A assistant. Answer the user's question briefly and clearly in the same language as the question (answer in Chinese if the question is in Chinese). Keep answers short. Do not use Markdown formatting because the game chat does not support it. Provide ONLY the answer itself. Never add follow-up offers, closing questions, or extra sentences such as 'if you have more questions', '有什么可以帮你的吗？', '有什么我可以帮你的吗？', '还需要我帮你什么吗？', or similar.";

	public String apiKey = "";
	public String apiUrl = "https://api.openai.com/v1/chat/completions";
	public String model = "gpt-3.5-turbo";
	public String systemPrompt = DEFAULT_SYSTEM_PROMPT;
	public int maxSuggestions = 3;
	public int requestCooldownMs = 500;
	public boolean enabled = true;

	public boolean qaEnabled = true;
	public boolean qaUseSameAi = true;
	public String qaApiKey = "";
	public String qaApiUrl = "";
	public String qaModel = "";
	public String qaSystemPrompt = DEFAULT_QA_SYSTEM_PROMPT;
	public int qaMaxTokens = 150;
	public String qaAnswerMode = "replace";
	public boolean qaStrictTrigger = true;
	public boolean debugLogging = true;
	public String apiMode = "mixed"; // mixed, api, fallback

	public static MojiDropConfig get() {
		if (INSTANCE == null) {
			load();
		}
		return INSTANCE;
	}

	public static void load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				INSTANCE = GSON.fromJson(reader, MojiDropConfig.class);
			} catch (Exception e) {
				INSTANCE = new MojiDropConfig();
			}
		} else {
			INSTANCE = new MojiDropConfig();
		}

		if (INSTANCE == null) {
			INSTANCE = new MojiDropConfig();
		}

		INSTANCE.validate();
		save();
	}

	public static void save() {
		if (INSTANCE == null) {
			return;
		}

		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to save MojiDrop config", e);
		}
	}

	private void validate() {
		if (apiKey == null) {
			apiKey = "";
		}
		if (apiUrl == null) {
			apiUrl = "https://api.openai.com/v1/chat/completions";
		}
		if (model == null) {
			model = "gpt-3.5-turbo";
		}
		if (systemPrompt == null || systemPrompt.isBlank()) {
			systemPrompt = DEFAULT_SYSTEM_PROMPT;
		}
		if (maxSuggestions < 1) {
			maxSuggestions = 1;
		}
		if (maxSuggestions > 5) {
			maxSuggestions = 5;
		}
		if (requestCooldownMs < 0) {
			requestCooldownMs = 0;
		}
		if (qaApiKey == null) {
			qaApiKey = "";
		}
		if (qaApiUrl == null) {
			qaApiUrl = "";
		}
		if (qaModel == null) {
			qaModel = "";
		}
		if (qaSystemPrompt == null || qaSystemPrompt.isBlank()) {
			qaSystemPrompt = DEFAULT_QA_SYSTEM_PROMPT;
		}
		if (qaMaxTokens < 1) {
			qaMaxTokens = 1;
		}
		if (qaMaxTokens > 2048) {
			qaMaxTokens = 2048;
		}
		if (qaAnswerMode == null || (!qaAnswerMode.equals("replace") && !qaAnswerMode.equals("append"))) {
			qaAnswerMode = "replace";
		}
		if (apiMode == null || (!apiMode.equals("mixed") && !apiMode.equals("api") && !apiMode.equals("fallback"))) {
			apiMode = "mixed";
		}
	}
}
