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

	public String apiKey = "";
	public String apiUrl = "https://api.openai.com/v1/chat/completions";
	public String model = "gpt-3.5-turbo";
	public int maxSuggestions = 3;
	public int requestCooldownMs = 500;
	public boolean enabled = true;

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
		if (maxSuggestions < 1) {
			maxSuggestions = 1;
		}
		if (maxSuggestions > 5) {
			maxSuggestions = 5;
		}
		if (requestCooldownMs < 0) {
			requestCooldownMs = 0;
		}
	}
}
