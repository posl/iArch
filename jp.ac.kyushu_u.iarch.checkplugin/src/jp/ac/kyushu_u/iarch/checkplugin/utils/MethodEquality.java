package jp.ac.kyushu_u.iarch.checkplugin.utils;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperMethod;

/**
 * Base class to provide equality function to Method and its containers.
 */
public abstract class MethodEquality {
	/**
	 * Test equality with Method.
	 * @param method
	 * @return true if condition is met.
	 */
	public abstract boolean match(Method method);

	/**
	 * Test equality with SuperMethod (OptMethod or AltMethod).
	 * When applied to AltMethod, check if one of methods meets the condition.
	 * @param superMethod
	 * @return true if condition is met.
	 */
	public boolean match(SuperMethod superMethod) {
		if (superMethod instanceof Method) {
			return match((Method) superMethod);
		} else if (superMethod instanceof OptMethod) {
			return match(((OptMethod) superMethod).getMethod());
		} else if (superMethod instanceof AltMethod) {
			for (Method method: ((AltMethod) superMethod).getMethods()) {
				if (match(method)) {
					return true;
				}
			}
			return false;
		}
		return false;
	}

	/**
	 * Test equality with SuperCall (CertainCall, OptCall or AltCall).
	 * When applied to AltCall, check if one of methods meets the condition.
	 * @param superCall
	 * @return true if condition is met.
	 */
	public boolean match(SuperCall superCall) {
		if (superCall instanceof CertainCall) {
			return match(((CertainCall) superCall).getName());
		} else if (superCall instanceof OptCall) {
			return match(((OptCall) superCall).getName());
		} else if (superCall instanceof AltCall) {
			AltCall altCall = (AltCall) superCall;
			if (match(altCall.getName())) {
				return true;
			}
			for (SuperMethod superMethod: altCall.getA_name()) {
				if (match(superMethod)) {
					return true;
				}
			}
			return false;
		}
		return false;
	}
}
