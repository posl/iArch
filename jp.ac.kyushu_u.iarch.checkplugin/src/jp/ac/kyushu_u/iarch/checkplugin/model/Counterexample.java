package jp.ac.kyushu_u.iarch.checkplugin.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Counterexample {

	private static final Pattern DeadlockPattern =
			Pattern.compile("Trace to (DEADLOCK):");
	private static final Pattern PropertyViolationPattern =
			Pattern.compile("Trace to property violation in ([^:]+):");
	private static final Pattern[] CounterexamplePatterns = {
		DeadlockPattern,
		PropertyViolationPattern,
	};

	private static final Pattern ActionPattern =
			Pattern.compile("^(\\S+)");

	// Extract counterexample from LTSA-PCA output.
	public static Counterexample create(String output) {
		String[] lines = output.split("\\r?\\n");

		for (int i = 0; i < lines.length; ++i) {
			String line = lines[i];

			String ceType = null;
			for (Pattern pattern : CounterexamplePatterns) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					ceType = matcher.group(1);
					break;
				}
			}

			if (ceType != null) {
				// Contents of a counterexample is indented by a tab.
				List<String> ceOutput = new ArrayList<String>();

				for (int j = i + 1; j < lines.length; ++j) {
					String ceLine = lines[j];
					int k = 0;
					for (; k < ceLine.length(); ++k) {
						if (!Character.isWhitespace(ceLine.charAt(k))) {
							break;
						}
					}
					if (k > 0) {
						ceOutput.add(ceLine.substring(k));
					} else {
						break;
					}
				}

				return new Counterexample(ceType, ceOutput);
			}
		}

		return null;
	}

	private String type;
	private List<String[]> actions;

	public Counterexample(String type, List<String> output) {
		this.type = type;
		actions = new ArrayList<String[]>();

		for (String line : output) {
			// extract first element.
			Matcher matcher = ActionPattern.matcher(line);
			if (matcher.find()) {
				String action = matcher.group(1);
				actions.add(action.split("\\."));
			}
		}
	}

	public String getType() {
		return type;
	}
	public List<String[]> getActions() {
		return actions;
	}

}
