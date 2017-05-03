package jp.ac.kyushu_u.iarch.classdiagram.utils;

import umlClass.DataType;
import umlClass.OptionalOperation;
import umlClass.UmlClassFactory;

public class OptionalOperationUtils {
	public static OptionalOperation createElement(String name, boolean isArchpoint) {
		OptionalOperation newOperation = UmlClassFactory.eINSTANCE.createOptionalOperation();
		newOperation.setArchpoint(isArchpoint);
		newOperation.setName(name);
		DataType newDataType = UmlClassFactory.eINSTANCE.createDataType();
		newDataType.setName("void");
		newOperation.setDatatype(newDataType);
		return newOperation;
	}

	public static String getLabel(OptionalOperation eOperation) {
		return "[ " + eOperation.getName() + "() ]";
	}
}
