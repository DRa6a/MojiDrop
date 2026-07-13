package mod.dra6a.client.service;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;

public class QaSuggestion extends Suggestion {
	private final String summary;
	private final String fullAnswer;

	public QaSuggestion(StringRange range, String summary, String fullAnswer) {
		super(range, summary);
		this.summary = summary;
		this.fullAnswer = fullAnswer;
	}

	@Override
	public String getText() {
		return this.summary;
	}

	@Override
	public String apply(String input) {
		if (this.getRange().getStart() >= input.length()) {
			return input + this.fullAnswer;
		}

		return input.substring(0, this.getRange().getStart()) + this.fullAnswer + input.substring(this.getRange().getEnd());
	}

	public String getFullAnswer() {
		return this.fullAnswer;
	}
}
