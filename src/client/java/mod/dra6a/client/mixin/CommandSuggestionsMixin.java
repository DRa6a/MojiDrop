package mod.dra6a.client.mixin;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import mod.dra6a.client.EmojiSuggestionDisplay;
import mod.dra6a.client.QaSuggestionDisplay;
import mod.dra6a.client.service.PlayerMentionService;
import mod.dra6a.client.service.QaService;
import mod.dra6a.client.service.QaSuggestion;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public class CommandSuggestionsMixin implements EmojiSuggestionDisplay, QaSuggestionDisplay {
	@Shadow
	private EditBox input;

	@Shadow
	private CompletableFuture<Suggestions> pendingSuggestions;

	@Shadow
	public void showSuggestions(boolean immediateNarration) {
	}

	@Override
	public void mojidrop$showEmojiSuggestions(List<String> emojis) {
		String text = this.input.getValue();
		StringRange range = StringRange.at(text.length());
		List<Suggestion> suggestionList = new ArrayList<>();

		for (String emoji : emojis) {
			suggestionList.add(new Suggestion(range, emoji));
		}

		this.pendingSuggestions = CompletableFuture.completedFuture(new Suggestions(range, suggestionList));
		this.showSuggestions(true);
	}

	@Override
	public void mojidrop$showQaSuggestion(String summary, String fullAnswer, int rangeStart, int rangeEnd) {
		StringRange range = new StringRange(rangeStart, rangeEnd);
		List<Suggestion> suggestionList = new ArrayList<>();
		suggestionList.add(new QaSuggestion(range, summary, fullAnswer));

		this.pendingSuggestions = CompletableFuture.completedFuture(new Suggestions(range, suggestionList));
		this.showSuggestions(true);
	}

	@Inject(method = "updateCommandInfo", at = @At("TAIL"))
	private void mojidrop$injectMentionSuggestions(CallbackInfo ci) {
		String value = this.input.getValue();
		int cursor = this.input.getCursorPosition();
		if (PlayerMentionService.shouldTrigger(value, cursor)) {
			this.pendingSuggestions = PlayerMentionService.buildMentionSuggestions(value, cursor);
			this.showSuggestions(true);
		}
	}
}
