package mod.dra6a.client.service;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class PlayerMentionService {

	public static CompletableFuture<Suggestions> buildMentionSuggestions(String input, int cursorPosition) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.player.connection == null) {
			return Suggestions.empty();
		}

		String partialCommand = input.substring(0, cursorPosition);
		int atIndex = partialCommand.lastIndexOf('@');
		if (atIndex < 0) {
			return Suggestions.empty();
		}

		String query = partialCommand.substring(atIndex + 1).toLowerCase(Locale.ROOT);
		StringRange range = StringRange.at(atIndex + 1);

		Collection<PlayerInfo> playerInfos = minecraft.player.connection.getOnlinePlayers();
		List<Suggestion> suggestions = new ArrayList<>();

		for (PlayerInfo info : playerInfos) {
			GameProfile profile = info.getProfile();
			if (profile == null || profile.name() == null) {
				continue;
			}

			String name = profile.name();
			if (query.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(query)) {
				suggestions.add(new Suggestion(range, name));
			}
		}

		suggestions.sort((a, b) -> a.getText().compareToIgnoreCase(b.getText()));
		return CompletableFuture.completedFuture(new Suggestions(range, suggestions));
	}

	public static boolean shouldTrigger(String input, int cursorPosition) {
		if (input == null || cursorPosition <= 0 || cursorPosition > input.length()) {
			return false;
		}

		String partialCommand = input.substring(0, cursorPosition);
		int atIndex = partialCommand.lastIndexOf('@');
		if (atIndex < 0) {
			return false;
		}

		String afterAt = partialCommand.substring(atIndex + 1);
		return !afterAt.contains(" ") && !partialCommand.trim().startsWith("/");
	}
}
