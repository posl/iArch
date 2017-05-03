package jp.ac.kyushu_u.iarch.sequencediagram.utils;

import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IInputValidator;

public class AlternativeMessageUtils {
	private static final Pattern methodNameSplitPattern = Pattern.compile("\\s+");

	public static String[] splitNames(String text) {
		String[] names = methodNameSplitPattern.split(text);
		for (String name: names) {
			if (!MessageUtils.validName(name)) {
				return null;
			}
		}
		return names;
	}

	public static String[] askMessageNames(String title, String message) {
		String str = MessageUtils.askString(title, message, "", new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText.length() == 0) {
					return "";
				} else {
					String[] names = splitNames(newText);
					if (names == null) {
						return "Invalid message names.";
					} else if (names.length <= 1) {
						return "Input more than one message names.";
					}
				}
				return null;
			}
		});
		return str != null ? splitNames(str) : null;
	}
}
