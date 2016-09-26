package jp.ac.kyushu.iarch.checkplugin.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.ArchDSLFactory;
import jp.ac.kyushu.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Param;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainInterface;

public class ArchModelUtils {
	//
	// Interface
	//

	public static Interface createInterfaceElement(String name) {
		Interface cInterface = ArchDSLFactory.eINSTANCE.createInterface();
		cInterface.setName(name);
		return cInterface;
	}

	public static Interface findInterfaceByName(Model model, String ifName) {
		for (Interface cInterface: model.getInterfaces()) {
			if (cInterface.getName().equals(ifName)) {
				return cInterface;
			}
		}
		return null;
	}

	//
	// Method
	//

	public static Method createMethodElement(Method originalMethod) {
		Method method = ArchDSLFactory.eINSTANCE.createMethod();
		method.setType(originalMethod.getType());
		method.setName(originalMethod.getName());
		for (Param originalParam: originalMethod.getParam()) {
			Param param = ArchDSLFactory.eINSTANCE.createParam();
			param.setType(originalParam.getType());
			param.setName(originalParam.getName());
			method.getParam().add(param);
		}
		return method;
	}

	public static Method createMethodElement(String methodName) {
		Method method = ArchDSLFactory.eINSTANCE.createMethod();
		method.setType("void");
		method.setName(methodName);
		return method;
	}

	public static Method createMethodElement(MethodDeclaration decl) {
		Method method = ArchDSLFactory.eINSTANCE.createMethod();
		method.setName(decl.getName().toString());
		// Return type
		method.setType(getMethodReturnType(decl));
		// Arguments
		for (Object obj: decl.parameters()) {
			SingleVariableDeclaration declParam = (SingleVariableDeclaration) obj;
			Param param = ArchDSLFactory.eINSTANCE.createParam();
			param.setType(getParameterType(declParam));
			param.setName(getParameterName(declParam));
			method.getParam().add(param);
		}
		return method;
	}

	public static Method createMethodElement(IMethodBinding binding) {
		Method method = ArchDSLFactory.eINSTANCE.createMethod();
		method.setName(binding.getName());
		// Return type
		method.setType(binding.getReturnType().getName());
		// Arguments
		for (ITypeBinding bindingParam: binding.getParameterTypes()) {
			Param param = ArchDSLFactory.eINSTANCE.createParam();
			param.setType(bindingParam.getName());
			// TODO: how can I get parameter names from IMethodBinding?
			param.setName("");
			method.getParam().add(param);
		}
		return method;
	}

	public static Method findMethodByName(Interface cInterface, String methodName) {
		for (Method method: cInterface.getMethods()) {
			if (method.getName().equals(methodName)) {
				return method;
			}
		}
		return null;
	}
	
	public static Method findMethodByName(UncertainInterface uInterface, String methodName) {
		for (AltMethod altMethod : uInterface.getAltmethods()) {
			for (Method method : altMethod.getMethods()) {
				if (method.getName().equals(methodName)) {
					return method;
				}
			}
		}
		for (OptMethod optMethod: uInterface.getOptmethods()) {
			Method method = optMethod.getMethod();
			if (method.getName().equals(methodName)) {
				return method;
			}
		}
		return null;
	}
	
	/**
	 * 特定のインターフェースとそのサブインターフェースの中から特定のメソッドを探す。
	 * @param model
	 * @param ifName
	 * @param methodName
	 * @return
	 */
	public static Method searchMethodByInterfaceAndName(Model model, String ifName, String methodName) {
		Method method;
		Interface cInterface = findInterfaceByName(model, ifName);
		if (cInterface != null) {
			method = findMethodByName(cInterface, methodName);
			if (method != null) {
				return method;
			}
		}
		for (UncertainInterface uInterface : searchUncertainInterfaceBySuperName(model, ifName)) {
			method = findMethodByName(uInterface, methodName);
			if (method != null) {
				return method;
			}
		}
		return null;
	}

	public static String getClassName(Method method) {
		return getClassName(method, true);
	}
	public static String getClassName(Method method, boolean allowUncertain) {
		EObject container = method.eContainer();
		if (container instanceof Interface) {
			return ((Interface) container).getName();
		} else if (allowUncertain
				&& (container instanceof OptMethod || container instanceof AltMethod)) {
			EObject uContainer = container.eContainer();
			if (uContainer instanceof UncertainInterface) {
				return ((UncertainInterface) uContainer).getSuperInterface().getName();
			}
		}
		return null;
	}

	//
	// UncertainInterface
	//

	public static UncertainInterface createUncertainInterfaceElement(String name, Interface cInterface) {
		UncertainInterface uInterface = ArchDSLFactory.eINSTANCE.createUncertainInterface();
		uInterface.setName(name);
		uInterface.setSuperInterface(cInterface);
		return uInterface;
	}

	public static UncertainInterface findUncertainInterfaceByName(Model model, String uIfName) {
		for (UncertainInterface uInterface: model.getU_interfaces()) {
			if (uInterface.getName().equals(uIfName)) {
				return uInterface;
			}
		}
		return null;
	}

	public static List<UncertainInterface> searchUncertainInterfaceBySuperName(Model model, String ifName) {
		ArrayList<UncertainInterface> uInterfaces = new ArrayList<UncertainInterface>();
		for (UncertainInterface uInterface: model.getU_interfaces()) {
			Interface cInterface = uInterface.getSuperInterface();
			if (cInterface != null && cInterface.getName().equals(ifName)) {
				uInterfaces.add(uInterface);
			}
		}
		return uInterfaces;
	}

	//
	// OptMethod
	//

	public static OptMethod createOptMethodElement(Method innerMethod) {
		OptMethod optMethod = ArchDSLFactory.eINSTANCE.createOptMethod();
		optMethod.setMethod(innerMethod);
		return optMethod;
	}

	public static OptMethod createOptMethodElement(String methodName) {
		return createOptMethodElement(createMethodElement(methodName));
	}

	public static OptMethod createOptMethodElement(MethodDeclaration decl) {
		return createOptMethodElement(createMethodElement(decl));
	}

	public static OptMethod createOptMethodElement(IMethodBinding binding) {
		return createOptMethodElement(createMethodElement(binding));
	}

	//
	// AltMethod
	//

	public static AltMethod createAltMethodElement(List<Method> innerMethods) {
		AltMethod altMethod = ArchDSLFactory.eINSTANCE.createAltMethod();
		altMethod.getMethods().addAll(innerMethods);
		return altMethod;
	}

	/**
	 * Create AltMethod from given AltCall.
	 * @return created AltMethod, or null if AltMethod contains other than (pure) Method.
	 */
	public static AltMethod createAltMethodElement(AltCall altCall) {
		AltMethod altMethod = ArchDSLFactory.eINSTANCE.createAltMethod();
		SuperMethod firstSuperMethod = altCall.getName();
		if (firstSuperMethod instanceof Method) {
			altMethod.getMethods().add((Method) firstSuperMethod);
			for (SuperMethod superMethod: altCall.getA_name()) {
				if (superMethod instanceof Method) {
					altMethod.getMethods().add((Method) superMethod);
				} else {
					return null;
				}
			}
		} else {
			return null;
		}
		return altMethod;
	}

	//
	// Behavior
	//

	/**
	 * Create Behavior from UncertainBehavior which has CertainCall only.<br>
	 * Note that CertainCall's Method is supported to be defined in certain Interface.
	 * @return Created Behavior, or null if failed.
	 */
	public static Behavior createBehaviorElement(UncertainBehavior uBehavior) {
		Behavior behavior = ArchDSLFactory.eINSTANCE.createBehavior();
		for (SuperCall superCall: uBehavior.getCall()) {
			if (superCall instanceof CertainCall) {
				SuperMethod superMethod = ((CertainCall) superCall).getName();
				if (superMethod instanceof Method) {
					behavior.getCall().add((Method) superMethod);
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		behavior.setInterface(uBehavior.getEnd());
		if (behavior.getCall().size() > 0) {
			behavior.setEnd(uBehavior.getEnd());
		}
		return behavior;
	}

	//
	// UncertainConnector
	//

	public static UncertainConnector createUncertainConnectorElement(String name, Connector connector) {
		UncertainConnector uConnector = ArchDSLFactory.eINSTANCE.createUncertainConnector();
		uConnector.setName(name);
		uConnector.setSuperInterface(connector);
		return uConnector;
	}

	public static UncertainConnector findUncertainConnectorByName(Model model, String ucName) {
		for (UncertainConnector uConnector: model.getU_connectors()) {
			if (uConnector.getName().equals(ucName)) {
				return uConnector;
			}
		}
		return null;
	}

	///
	/// UncertainBehavior
	//

	public static boolean hasUncertainty(UncertainBehavior uBehavior) {
		for (SuperCall superCall: uBehavior.getCall()) {
			if (!(superCall instanceof CertainCall)) {
				return true;
			}
		}
		return false;
	}

	// Generate unique name for UncertainBehavior (but why?)
	public static String generateUncertainBehaviorName(UncertainConnector uConnector) {
		for (int i = 0; ; ++i) {
			String name = uConnector.getName() + "_ub_" + String.valueOf(i);
			boolean isUnique = true;
			for (UncertainBehavior ub: uConnector.getU_behaviors()) {
				if (ub.getName().equals(name)) {
					isUnique = false;
					break;
				}
			}
			if (isUnique) {
				return name;
			}
		}
	}

	//
	// SuperCall
	//

	public static CertainCall createCertainCallElement(SuperMethod superMethod) {
		CertainCall certainCall = ArchDSLFactory.eINSTANCE.createCertainCall();
		certainCall.setName(superMethod);
		return certainCall;
	}

	public static OptCall createOptCallElement(SuperMethod superMethod) {
		OptCall optCall = ArchDSLFactory.eINSTANCE.createOptCall();
		optCall.setName(superMethod);
		return optCall;
	}

	public static AltCall createAltCallElement(AltMethod altMethod) {
		AltCall altCall = ArchDSLFactory.eINSTANCE.createAltCall();
		EList<Method> methods = altMethod.getMethods();
		for (int j = 0; j < methods.size(); ++j) {
			if (j == 0) {
				altCall.setName(methods.get(j));
			} else {
				altCall.getA_name().add(methods.get(j));
			}
		}
		return altCall;
	}

	/**
	 * Get Method from SuperCall if it is CertainCall.
	 * @return (pure) Method within CertainCall, otherwise null.
	 */
	public static Method getMethodIfCertain(SuperCall superCall) {
		if (superCall instanceof CertainCall) {
			SuperMethod superMethod = ((CertainCall) superCall).getName();
			if (superMethod instanceof Method) {
				return (Method) superMethod;
			}
		}
		return null;
	}

	//
	// Method declaration from AST.
	//

	public static String getMethodReturnType(MethodDeclaration decl) {
		Type returnType = decl.getReturnType2();
		return (returnType != null) ? returnType.toString() : "void";
	}
	public static String getParameterType(SingleVariableDeclaration parameter) {
		return parameter.getType().toString();
	}
	public static String getParameterName(SingleVariableDeclaration parameter) {
		return parameter.getName().toString();
	}

	//
	// Auto UncertainInterface/Connector name.
	//

	public static String getAutoUncertainInterfaceName(String interfaceName) {
		return "u" + interfaceName + "_auto";
	}
	public static String getAutoUncertainConnectorName(String connectorName) {
		return "u" + connectorName + "_auto";
	}
}
