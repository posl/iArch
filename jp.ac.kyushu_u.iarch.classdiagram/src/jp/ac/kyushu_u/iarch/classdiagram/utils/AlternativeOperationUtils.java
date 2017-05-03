package jp.ac.kyushu_u.iarch.classdiagram.utils;

import java.util.regex.Pattern;

import umlClass.AlternativeOperation;
import umlClass.Operation;

public class AlternativeOperationUtils {
	private static final Pattern methodNameSplitPattern = Pattern.compile("\\s+");

	public static String[] splitNames(String text) {
		String[] names = methodNameSplitPattern.split(text);
		for (String name: names) {
			if (!OperationUtils.validName(name)) {
				return null;
			}
		}
		return names;
	}

	public static String getJoinedNames(AlternativeOperation eOperation) {
		StringBuilder sb = new StringBuilder();
		for (Operation op: eOperation.getOperations()) {
			sb.append(op.getName()).append(" ");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public static String getLabel(AlternativeOperation eOperation) {
		StringBuilder sb = new StringBuilder();
		sb.append("{ ");
		for (Operation op: eOperation.getOperations()) {
			sb.append(op.getName()).append("() ");
		}
		sb.append("}");
		return sb.toString();
	}

	public static boolean hasName(AlternativeOperation eOperation, String name) {
		for (Operation op: eOperation.getOperations()) {
			if (name.equals(op.getName())) {
				return true;
			}
		}
		return false;
	}

	public static boolean sameNames(AlternativeOperation eOperation, String[] names) {
		if (eOperation.getOperations().size() != names.length) {
			return false;
		}
		for (Operation op: eOperation.getOperations()) {
			String opName = op.getName();
			boolean nameFound = false;
			for (String name: names) {
				if (name.equals(opName)) {
					nameFound = true;
					break;
				}
			}
			if (!nameFound) {
				return false;
			}
		}
		return true;
	}
}
