package jp.ac.kyushu.iarch.basefunction.model;

import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
//import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainInterface;

/**
 * A class of Archface-U component for type check.
 */
public class ComponentTypeCheckModel {
	public static class MethodModel {
		private Method method;
		private boolean optional;
		private boolean alternative;

		private MethodModel(Method method, boolean optional, boolean alternative) {
			this.method = method;
			this.optional = optional;
			this.alternative = alternative;
		}

		public Method getMethod() {
			return method;
		}
		public boolean isOptional() {
			return optional;
		}
		public boolean isAlternative() {
			return alternative;
		}
		public boolean isCertain() {
			return !(optional || alternative);
		}

		private void setOptional(boolean optional) {
			this.optional = optional;
		}
		private void setAlternative(boolean alternative) {
			this.alternative = alternative;
		}
	}

	private String name;
	private Map<String, MethodModel> methodModels;
	private List<AltMethod> altMethods;
//	private Map<String, List<AltMethod>> altMethodMap;

	private ComponentTypeCheckModel(String name, List<Method> methods,
			List<OptMethod> optMethods, List<AltMethod> altMethods) {
		this.name = name;
		this.methodModels = new HashMap<String, MethodModel>();
		this.altMethods = new ArrayList<AltMethod>(altMethods);
//		this.altMethodMap = new HashMap<String, List<AltMethod>>();

		for (Method m : methods) {
			methodModels.put(m.getName(), new MethodModel(m, false, false));
		}
		for (OptMethod om : optMethods) {
			String omName = om.getMethod().getName();
			MethodModel mm = methodModels.get(omName);
			if (mm == null) {
				methodModels.put(omName, new MethodModel(om.getMethod(), true, false));
			} else {
				mm.setOptional(true);
			}
		}
		for (AltMethod am : altMethods) {
			for (Method m : am.getMethods()) {
				String amName = m.getName();
				MethodModel mm = methodModels.get(amName);
				if (mm == null) {
					methodModels.put(amName, new MethodModel(m, false, true));
				} else {
					mm.setAlternative(true);
				}
//				List<AltMethod> ams = altMethodMap.get(amName);
//				if (ams == null) {
//					ams = new ArrayList<AltMethod>();
//					altMethodMap.put(amName, ams);
//				}
//				ams.add(am);
			}
		}
	}

	public String getName() {
		return name;
	}

	public Collection<MethodModel> getMethodModels() {
		return methodModels.values();
	}

	public MethodModel getMethodModel(String methodName) {
		return methodModels.get(methodName);
	}

	public Collection<AltMethod> getAltMethods() {
		return altMethods;
	}

	public AltMethod getAltMethod(Set<String> methodNames) {
		for (AltMethod am : altMethods) {
			if (am.getMethods().size() == methodNames.size()) {
				boolean hasMethod = true;
				for (Method m : am.getMethods()) {
					if (!methodNames.contains(m.getName())) {
						hasMethod = false;
						break;
					}
				}
				if (hasMethod) {
					return am;
				}
			}
		}
		return null;
	}

//	public List<Method> searchUnfulfilledCertainMethods(Set<String> methodNames) {
//		ArrayList<Method> result = new ArrayList<Method>();
//		for (MethodModel mm : methodModels.values()) {
//			if (mm.isCertain()) {
//				Method m = mm.getMethod();
//				if (!methodNames.contains(m.getName())) {
//					result.add(m);
//				}
//			}
//		}
//		return result;
//	}
//	public List<Method> searchUnfulfilledCertainMethods(List<String> methodNames) {
//		return searchUnfulfilledCertainMethods(new HashSet<String>(methodNames));
//	}
//	public List<Method> searchUnfulfilledCertainMethods(String[] methodNames) {
//		return searchUnfulfilledCertainMethods(new HashSet<String>(Arrays.asList(methodNames)));
//	}
//
//	public List<AltMethod> searchUnfulfilledAltMethods(Set<String> methodNames) {
//		ArrayList<AltMethod> result = new ArrayList<AltMethod>();
//		for (AltMethod am : altMethods) {
//			for (Method m : am.getMethods()) {
//				if (!methodNames.contains(m.getName())) {
//					result.add(am);
//					break;
//				}
//			}
//		}
//		return result;
//	}
//	public List<AltMethod> searchUnfulfilledAltMethods(List<String> methodNames) {
//		return searchUnfulfilledAltMethods(new HashSet<String>(methodNames));
//	}
//	public List<AltMethod> searchUnfulfilledAltMethods(String[] methodNames) {
//		return searchUnfulfilledAltMethods(new HashSet<String>(Arrays.asList(methodNames)));
//	}

	public static ComponentTypeCheckModel getTypeCheckModel(Model model, String name) {
		Interface cInterface = null;
		for (Interface ci : model.getInterfaces()) {
			if (ci.getName().equals(name)) {
				cInterface = ci;
				break;
			}
		}
		if (cInterface != null) {
			ArrayList<Method> methods = new ArrayList<Method>(cInterface.getMethods());
			ArrayList<OptMethod> optMethods = new ArrayList<OptMethod>();
			ArrayList<AltMethod> altMethods = new ArrayList<AltMethod>();
			for (UncertainInterface uInterface : model.getU_interfaces()) {
				Interface si = uInterface.getSuperInterface();
				if (si != null && si.getName().equals(name)) {
					optMethods.addAll(uInterface.getOptmethods());
					altMethods.addAll(uInterface.getAltmethods());
				}
			}
			return new ComponentTypeCheckModel(name, methods, optMethods, altMethods);
		} else {
			return null;
		}
	}
}
