package jp.ac.kyushu_u.iarch.classdiagram.utils;

import java.util.regex.Pattern;

import umlClass.DataType;
import umlClass.Operation;
import umlClass.UmlClassFactory;

public class OperationUtils {
	private static final Pattern methodNamePattern = Pattern.compile("[a-zA-Z_]\\w*");

	public static boolean validName(String name) {
		return methodNamePattern.matcher(name).matches();
	}

	public static Operation createElement(String name, boolean isArchpoint) {
		Operation newOperation = UmlClassFactory.eINSTANCE.createOperation();
		newOperation.setArchpoint(isArchpoint);
		newOperation.setName(name);
		DataType newDataType = UmlClassFactory.eINSTANCE.createDataType();
		newDataType.setName("void");
		newOperation.setDatatype(newDataType);
		return newOperation;
	}

	public static String getLabel(Operation eOperation) {
		return eOperation.getName() + "()";
	}
}
