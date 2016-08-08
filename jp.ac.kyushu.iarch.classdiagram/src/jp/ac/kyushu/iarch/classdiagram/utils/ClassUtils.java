package jp.ac.kyushu.iarch.classdiagram.utils;

import java.util.ArrayList;

import umlClass.AlternativeOperation;
import umlClass.Operation;
import umlClass.OptionalOperation;

public class ClassUtils {
	public static int calculateHeight(umlClass.Class eClass) {
		return (eClass.getAttribute().size() + eClass.getOwnedOperation().size()) * 20 + 32;
	}

	public static int calculateOperationBaseline(umlClass.Class eClass, Operation eOperation) {
		int i = 0;
		for (Operation op: eClass.getOwnedOperation()) {
			if (op == eOperation) {
				break;
			}
			i++;
		}
		return (eClass.getAttribute().size() + i) * 20 + 32;
	}

	public static String[] getChildNames(umlClass.Class eClass) {
		ArrayList<String> names = new ArrayList<String>();
		for (Operation eOperation: eClass.getOwnedOperation()) {
			if (eOperation instanceof AlternativeOperation) {
				for (Operation op: ((AlternativeOperation) eOperation).getOperations()) {
					names.add(op.getName());
				}
			} else {
				names.add(eOperation.getName());
			}
		}
		return names.toArray(new String[names.size()]);
	}

	public static boolean hasName(umlClass.Class eClass, String name) {
		for (Operation eOperation: eClass.getOwnedOperation()) {
			if (eOperation instanceof AlternativeOperation) {
				if (AlternativeOperationUtils.hasName((AlternativeOperation) eOperation, name)) {
					return true;
				}
			} else {
				if (name.equals(eOperation.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasCertainName(umlClass.Class eClass, String name) {
		for (Operation eOperation: eClass.getOwnedOperation()) {
			if (!(eOperation instanceof OptionalOperation)
					&& !(eOperation instanceof AlternativeOperation)) {
				if (name.equals(eOperation.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasOptName(umlClass.Class eClass, String name) {
		for (Operation eOperation: eClass.getOwnedOperation()) {
			if (eOperation instanceof OptionalOperation) {
				if (name.equals(eOperation.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasAltName(umlClass.Class eClass, String[] names) {
		for (Operation eOperation: eClass.getOwnedOperation()) {
			if (eOperation instanceof AlternativeOperation) {
				if (AlternativeOperationUtils.sameNames((AlternativeOperation) eOperation, names)) {
					return true;
				}
			}
		}
		return false;
	}
}
