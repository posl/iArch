package jp.ac.kyushu.iarch.checkplugin.model;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperMethod;

public final class Utility {

	private Utility() {}

	public static String getReprName(SuperMethod method) {
		if (method instanceof Method) {
			return ((Method) method).getName();
		} else if (method instanceof OptMethod) {
			return ((OptMethod) method).getMethod().getName();
		} else if (method instanceof AltMethod) {
			return ((AltMethod) method).getMethods().get(0).getName();
		} else {
			throw new IllegalArgumentException();
		}
	}
	
}
