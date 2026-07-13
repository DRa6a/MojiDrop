package mod.dra6a.client.service;

import mod.dra6a.client.config.MojiDropConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugLogService {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final Path LOG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mojidrop/debug.log");

	public static void log(String type, String input, String output) {
		if (!MojiDropConfig.get().debugLogging) {
			return;
		}

		try {
			Files.createDirectories(LOG_PATH.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(LOG_PATH, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
				writer.write("[" + LocalDateTime.now().format(FORMATTER) + "] [" + type + "]");
				writer.newLine();
				writer.write("Input: " + input.replace("\n", "\\n"));
				writer.newLine();
				writer.write("Output: " + output.replace("\n", "\\n"));
				writer.newLine();
				writer.write("---");
				writer.newLine();
			}
		} catch (IOException e) {
			// Silently ignore logging failures to avoid breaking gameplay
		}
	}
}
