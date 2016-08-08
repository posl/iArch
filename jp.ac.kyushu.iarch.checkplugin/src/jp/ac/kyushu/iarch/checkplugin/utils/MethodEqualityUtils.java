package jp.ac.kyushu.iarch.checkplugin.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Param;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperMethod;

public class MethodEqualityUtils {
	//
	// MethodEquality classes
	//

	// Check chain
	private static class MethodEqualityChain extends MethodEquality {
		private MethodEquality[] chain;

		private MethodEqualityChain(MethodEquality[] chain) {
			this.chain = chain;
		}

		@Override
		public boolean match(Method method) {
			for (MethodEquality equality: chain) {
				if (!equality.match(method)) {
					return false;
				}
			}
			return true;
		}
		@Override
		public boolean match(SuperMethod superMethod) {
			for (MethodEquality equality: chain) {
				if (!equality.match(superMethod)) {
					return false;
				}
			}
			return true;
		}
		@Override
		public boolean match(SuperCall superCall) {
			for (MethodEquality equality: chain) {
				if (!equality.match(superCall)) {
					return false;
				}
			}
			return true;
		}
	}

	// Equals to any method, even if null.
	public static MethodEquality anyMethod = new MethodEquality() {
		@Override
		public boolean match(Method method) {
			return true;
		}
		@Override
		public boolean match(SuperMethod superMethod) {
			return true;
		}
		@Override
		public boolean match(SuperCall superCall) {
			return true;
		}
	};

	// Equals to null.
	public static MethodEquality nullMethod = new MethodEquality() {
		@Override
		public boolean match(Method method) {
			return method == null;
		}
		@Override
		public boolean match(SuperMethod superMethod) {
			return superMethod == null;
		}
		@Override
		public boolean match(SuperCall superCall) {
			return superCall == null;
		}
	};

	// Check the name of class it belongs.
	private static class MethodEqualityByClass extends MethodEquality {
		private String className;
		private boolean allowUncertain;

		private MethodEqualityByClass(String className, boolean allowUncertain) {
			this.className = className;
			this.allowUncertain = allowUncertain;
		}

		@Override
		public boolean match(Method method) {
			if (method == null) {
				return false;
			}
			if (!ArchModelUtils.getClassName(method, allowUncertain).equals(className)) {
				return false;
			}
			return true;
		}
	}
	private static class MethodEqualityByClasses extends MethodEquality {
		private String[] classNames;
		private boolean allowUncertain;

		private MethodEqualityByClasses(String[] classNames, boolean allowUncertain) {
			this.classNames = classNames;
			this.allowUncertain = allowUncertain;
		}

		@Override
		public boolean match(Method method) {
			if (method == null) {
				return false;
			}
			String methodClassName = ArchModelUtils.getClassName(method, allowUncertain);
			for (String className : classNames) {
				if (className.equals(methodClassName)) {
					return true;
				}
			}
			return false;
		}
	}

	// Check the name of method.
	private static class MethodEqualityByName extends MethodEquality {
		private String methodName;

		private MethodEqualityByName(String methodName) {
			this.methodName = methodName;
		}

		@Override
		public boolean match(Method method) {
			if (method == null) {
				return false;
			}
			if (!method.getName().equals(methodName)) {
				return false;
			}
			return true;
		}
	}
	public static MethodEquality createMethodEquality(String methodName) {
		return new MethodEqualityByName(methodName);
	}
	public static MethodEquality createMethodEquality(String className, String methodName) {
		return new MethodEqualityChain(new MethodEquality[] {
				new MethodEqualityByClass(className, true),
				createMethodEquality(methodName)
		});
	}
	public static MethodEquality createMethodEquality(String[] classNames, String methodName) {
		return new MethodEqualityChain(new MethodEquality[] {
				new MethodEqualityByClasses(classNames, true),
				createMethodEquality(methodName)
		});
	}

	// Check based on other Method
	private static class MethodEqualityByMethod extends MethodEquality {
		private Method baseMethod;
		private boolean strict;

		private MethodEqualityByMethod(Method method, boolean strict) {
			this.baseMethod = method;
			this.strict = strict;
		}

		@Override
		public boolean match(Method method) {
			if (baseMethod == null) {
				return method == null;
			}
			if (method == null) {
				return false;
			}
			if (!baseMethod.getName().equals(method.getName())) {
				return false;
			}
			if (strict) {
				// Return type
				if (!baseMethod.getType().equals(method.getType())) {
					return false;
				}
				// Argument size
				EList<Param> baseParams = baseMethod.getParam();
				EList<Param> params = method.getParam();
				if (baseParams.size() != params.size()) {
					return false;
				}
				// Arguments
				for (int i = 0; i < baseParams.size(); ++i) {
					Param baseParam = baseParams.get(i);
					Param param = params.get(i);
					if (!baseParam.getType().equals(param.getType())) {
						return false;
					}
					if (!baseParam.getName().equals(param.getName())) {
						return false;
					}
				}
			}
			return true;
		}
	}
	public static MethodEquality createMethodEquality(Method method) {
		// For now, strict check is suppressed.
		return new MethodEqualityByMethod(method, false);
	}
	public static MethodEquality createMethodEquality(String className, Method method) {
		return new MethodEqualityChain(new MethodEquality[] {
				new MethodEqualityByClass(className, true),
				createMethodEquality(method)
		});
	}

	// Check based on MethodDeclaration from AST.
	private static class MethodEqualityByDecl extends MethodEquality {
		private MethodDeclaration decl;
		private boolean strict;

		private MethodEqualityByDecl(MethodDeclaration decl, boolean strict) {
			this.decl = decl;
			this.strict = strict;
		}

		@Override
		public boolean match(Method method) {
			if (decl == null || method == null) {
				return false;
			}
			if (!decl.getName().toString().equals(method.getName())) {
				return false;
			}
			if (strict) {
				// Return type
				String returnTypeString = ArchModelUtils.getMethodReturnType(decl);
				if (!returnTypeString.equals(method.getType())) {
					return false;
				}
				// Argument size
				List<?> declParams = decl.parameters();
				EList<Param> params = method.getParam();
				if (declParams.size() != params.size()) {
					return false;
				}
				// Arguments
				for (int i = 0; i < declParams.size(); ++i) {
					SingleVariableDeclaration declParam = (SingleVariableDeclaration) declParams.get(i);
					Param param = params.get(i);
					if (!ArchModelUtils.getParameterType(declParam).equals(param.getType())) {
						return false;
					}
					if (!ArchModelUtils.getParameterName(declParam).equals(param.getName())) {
						return false;
					}
				}
			}
			return true;
		}
	}
	public static MethodEquality createMethodEquality(MethodDeclaration decl) {
		// For now, strict check is suppressed.
		return new MethodEqualityByDecl(decl, false);
	}
	public static MethodEquality createMethodEquality(String className, MethodDeclaration decl) {
		return new MethodEqualityChain(new MethodEquality[] {
				new MethodEqualityByClass(className, true),
				createMethodEquality(decl)
		});
	}

	// Check based on method binding obtained by resolving invocation.
	private static class MethodEqualityByBinding extends MethodEquality {
		private IMethodBinding binding;
		private boolean strict;

		private MethodEqualityByBinding(IMethodBinding binding, boolean strict) {
			this.binding = binding;
			this.strict = strict;
		}

		@Override
		public boolean match(Method method) {
			if (binding == null || method == null) {
				return false;
			}
			if (!binding.getName().equals(method.getName())) {
				return false;
			}
			if (strict) {
				// Return type
				String returnTypeString = binding.getReturnType().getName();
				if (!returnTypeString.equals(method.getType())) {
					return false;
				}
				// Argument size
				ITypeBinding[] bindingParams = binding.getParameterTypes();
				EList<Param> params = method.getParam();
				if (bindingParams.length != params.size()) {
					return false;
				}
				// Arguments
				for (int i = 0; i < bindingParams.length; ++i) {
					Param param = params.get(i);
					if (!bindingParams[i].getName().equals(param.getType())) {
						return false;
					}
					// TODO: how can I get parameter names from IMethodBinding?
				}
			}
			return true;
		}
	}
	public static MethodEquality createMethodEquality(IMethodBinding binding) {
		// For now, strict check is suppressed.
		return new MethodEqualityByBinding(binding, false);
	}
	public static MethodEquality createMethodEquality(String className, IMethodBinding binding) {
		return new MethodEqualityChain(new MethodEquality[] {
				new MethodEqualityByClass(className, true),
				createMethodEquality(binding)
		});
	}

	// Only matches for OptMethod/OptCall.
	private static class MethodEqualityForOpt extends MethodEquality {
		private MethodEquality equality;

		private MethodEqualityForOpt(MethodEquality equality) {
			this.equality = equality;
		}

		@Override
		public boolean match(Method method) {
			return false;
		}
		@Override
		public boolean match(SuperMethod superMethod) {
			if (superMethod instanceof OptMethod) {
				return equality.match(((OptMethod) superMethod).getMethod());
			}
			return false;
		}
		@Override
		public boolean match(SuperCall superCall) {
			if (superCall instanceof OptCall) {
				return equality.match(((OptCall) superCall).getName());
			}
			return false;
		}
	}
	public static MethodEquality createMethodEqualityForOpt(MethodEquality equality) {
		return new MethodEqualityForOpt(equality);
	}

	//
	// Equality for AltMethod/AltCall
	//

	public static List<MethodEquality> createAltMethodEquality(List<Method> alternatives, boolean checkClass) {
		ArrayList<MethodEquality> equalities = new ArrayList<MethodEquality>();
		for (Method method: alternatives) {
			if (checkClass) {
				String className = ArchModelUtils.getClassName(method);
				equalities.add(createMethodEquality(className, method));
			} else {
				equalities.add(createMethodEquality(method));
			}
		}
		return equalities;
	}
	public static List<MethodEquality> createAltMethodEquality(String className, List<Method> alternatives) {
		ArrayList<MethodEquality> equalities = new ArrayList<MethodEquality>();
		for (Method method: alternatives) {
			equalities.add(createMethodEquality(className, method));
		}
		return equalities;
	}

	public static List<MethodEquality> createAltMethodEquality(AltMethod altMethod, boolean checkClass) {
		return createAltMethodEquality(altMethod.getMethods(), checkClass);
	}
	public static List<MethodEquality> createAltMethodEquality(String className, AltMethod altMethod) {
		return createAltMethodEquality(className, altMethod.getMethods());
	}

	// Note that AltCall is consider not to contain OptMethod nor AltMethod.
	public static List<MethodEquality> createAltMethodEquality(AltCall altCall, boolean checkClass) {
		ArrayList<MethodEquality> equalities = new ArrayList<MethodEquality>();
		SuperMethod firstSuperMethod = altCall.getName();
		if (firstSuperMethod instanceof Method) {
			Method firstMethod = (Method) firstSuperMethod;
			if (checkClass) {
				String className = ArchModelUtils.getClassName(firstMethod);
				equalities.add(createMethodEquality(className, firstMethod));
			} else {
				equalities.add(createMethodEquality(firstMethod));
			}
		} else {
			return null;
		}
		for (SuperMethod superMethod: altCall.getA_name()) {
			if (superMethod instanceof Method) {
				Method method = (Method) superMethod;
				if (checkClass) {
					String className = ArchModelUtils.getClassName(method);
					equalities.add(createMethodEquality(className, method));
				} else {
					equalities.add(createMethodEquality(method));
				}
			} else {
				return null;
			}
		}
		return equalities;
	}
	public static List<MethodEquality> createAltMethodEquality(String className, AltCall altCall) {
		ArrayList<MethodEquality> equalities = new ArrayList<MethodEquality>();
		SuperMethod firstSuperMethod = altCall.getName();
		if (firstSuperMethod instanceof Method) {
			equalities.add(createMethodEquality(className, (Method) firstSuperMethod));
		} else {
			return null;
		}
		for (SuperMethod superMethod: altCall.getA_name()) {
			if (superMethod instanceof Method) {
				equalities.add(createMethodEquality(className, (Method) superMethod));
			} else {
				return null;
			}
		}
		return equalities;
	}

	public static boolean matchAltMethod(List<MethodEquality> equalities, AltMethod altMethod) {
		if (equalities.size() != altMethod.getMethods().size()) {
			return false;
		}
		for (MethodEquality equality: equalities) {
			boolean methodFound = false;
			for (Method method: altMethod.getMethods()) {
				if (equality.match(method)) {
					methodFound = true;
					break;
				}
			}
			if (!methodFound) {
				return false;
			}
		}
		return true;
	}

	public static boolean matchAltCall(List<MethodEquality> equalities, AltCall altCall) {
		if (equalities.size() != altCall.getA_name().size() + 1) {
			return false;
		}
		for (MethodEquality equality: equalities) {
			boolean methodFound = false;
			SuperMethod firstSuperMethod = altCall.getName();
			if (equality.match(firstSuperMethod)) {
				methodFound = true;
			}
			if (!methodFound) {
				for (SuperMethod superMethod: altCall.getA_name()) {
					if (equality.match(superMethod)) {
						methodFound = true;
						break;
					}
				}
			}
			if (!methodFound) {
				return false;
			}
		}
		return true;
	}

	// Only matches for AltMethod/AltCall.
	private static class MethodEqualityForAlt extends MethodEquality {
		private List<MethodEquality> equalities;

		private MethodEqualityForAlt(List<MethodEquality> equalities) {
			this.equalities = equalities;
		}

		@Override
		public boolean match(Method method) {
			return false;
		}
		@Override
		public boolean match(SuperMethod superMethod) {
			if (superMethod instanceof AltMethod) {
				return matchAltMethod(equalities, (AltMethod) superMethod);
			}
			return false;
		}
		@Override
		public boolean match(SuperCall superCall) {
			if (superCall instanceof AltCall) {
				return matchAltCall(equalities, (AltCall) superCall);
			}
			return false;
		}
	}
	public static MethodEquality createMethodEqualityForAlt(List<MethodEquality> equalities) {
		return new MethodEqualityForAlt(equalities);
	}

	//
	// Sameness check
	//

	public static boolean sameMethod(Method m1, Method m2, boolean checkClass) {
		if (checkClass) {
			String className = ArchModelUtils.getClassName(m1);
			return createMethodEquality(className, m1).match(m2);
		} else {
			return createMethodEquality(m1).match(m2);
		}
	}

	public static boolean sameSuperMethod(SuperMethod sm1, SuperMethod sm2, boolean checkClass) {
		if (sm1 instanceof Method) {
			if (sm2 instanceof Method) {
				return sameMethod((Method) sm1, (Method) sm2, checkClass);
			}
		} else if (sm1 instanceof OptMethod) {
			if (sm2 instanceof OptMethod) {
				Method m1 = ((OptMethod) sm1).getMethod();
				Method m2 = ((OptMethod) sm2).getMethod();
				return sameMethod(m1, m2, checkClass);
			}
		} else if (sm1 instanceof AltMethod) {
			if (sm2 instanceof AltMethod) {
				List<MethodEquality> equalities = createAltMethodEquality((AltMethod) sm1, checkClass);
				return matchAltMethod(equalities, (AltMethod) sm2);
			}
		}
		return false;
	}

	public static boolean sameSuperCall(SuperCall sc1, SuperCall sc2, boolean checkClass) {
		if (sc1 instanceof CertainCall) {
			if (sc2 instanceof CertainCall) {
				return sameSuperMethod(sc1.getName(), sc2.getName(), checkClass);
			}
		} else if (sc1 instanceof OptCall) {
			if (sc2 instanceof OptCall) {
				return sameSuperMethod(sc1.getName(), sc2.getName(), checkClass);
			}
		} else if (sc1 instanceof AltCall) {
			if (sc2 instanceof AltCall) {
				List<MethodEquality> equalities = createAltMethodEquality((AltCall) sc1, checkClass);
				return matchAltCall(equalities, (AltCall) sc2);
			}
		}
		return false;
	}
}
