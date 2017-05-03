package jp.ac.kyushu_u.iarch.checkplugin.handler;

import java.util.Iterator;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu_u.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu_u.iarch.checkplugin.utils.ArchModelUtils;
import jp.ac.kyushu_u.iarch.checkplugin.utils.DiagramUtils;
import jp.ac.kyushu_u.iarch.checkplugin.utils.MethodEquality;
import jp.ac.kyushu_u.iarch.checkplugin.utils.MethodEqualityUtils;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class RemoveUncertaintyAsNecessaryHandler extends AbstractUncertaintyOperationHandler {
	@Override
	protected void operateCodeMethodDeclaration(ExecutionEvent event, MethodDeclarationInfo declInfo,
			ArchModel archModel) throws ExecutionException {
		String className = declInfo.typeName();
		String methodName = declInfo.methodName();
		Model model = archModel.getModel();

		boolean archModelModified = false;
		try {
			if (declInfo.inheritingTypes != null) {
				for (IType type : declInfo.inheritingTypes) {
					String inhClassName = type.getElementName();
					MethodEquality nameEquality = MethodEqualityUtils.createMethodEquality(inhClassName, methodName);
					archModelModified |= removeAsNecessary(inhClassName, nameEquality, declInfo.decl, model);
				}
			} else {
				MethodEquality nameEquality = MethodEqualityUtils.createMethodEquality(className, methodName);
				archModelModified |= removeAsNecessary(className, nameEquality, declInfo.decl, model);
			}
		} catch (ModelErrorException e) {
			System.out.println("RemoveUncertaintyAsNecessaryHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			saveArchModel(archModel);
		}
	}
	private boolean removeAsNecessary(String className, MethodEquality nameEquality,
				MethodDeclaration decl, Model model) throws ModelErrorException {
		Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
		if (cInterface == null) {
			throw new ModelErrorException("Target interface does not exist: " + className);
		}
		boolean modified = false;

		Method method = getMethod(cInterface, decl);
//		boolean methodCreated = false;
		if (method == null) {
			Method newMethod = ArchModelUtils.createMethodElement(decl);
			cInterface.getMethods().add(newMethod);
			method = newMethod;
			// Do not turn on modification flag
			// because new Method is needed only when behaviors are modified.
//			methodCreated = true;
		}

		modified |= modifyUncertainCallToCertain(model, MethodEqualityUtils.anyMethod, nameEquality, method);

		MethodEquality methodEquality = MethodEqualityUtils.createMethodEquality(method);
		modified |= removeUnusedUncertainMethod(model, className, methodEquality, true);

//		if (methodCreated && !modified) {
//			// Discard unused Method.
//			EObject container = method.eContainer();
//			if (container instanceof Interface) {
//				((Interface) container).getMethods().remove(method);
//			} else {
//				throw new ModelErrorException("Failed to discard unused Method: " + className);
//			}
//		}

		return modified;
	}

	private Method getMethod(Interface cInterface, MethodDeclaration decl) {
		MethodEquality equality = MethodEqualityUtils.createMethodEquality(decl);
		return getMethod(cInterface, equality);
	}

	private boolean modifyUncertainCallToCertain(Model model,
			MethodEquality callerEquality, MethodEquality calleeEquality,
			Method method) throws ModelErrorException {
		boolean modified = false;

		for (UncertainConnector uConnector: model.getU_connectors()) {
			Iterator<UncertainBehavior> uBehaviorIt = uConnector.getU_behaviors().iterator();
			while (uBehaviorIt.hasNext()) {
				UncertainBehavior uBehavior = uBehaviorIt.next();
				EList<SuperCall> superCalls = uBehavior.getCall();

				boolean uBehaviorModified = false;
				for (int i = 0; i < superCalls.size(); ++i) {
					SuperCall caller = (i > 0) ? superCalls.get(i - 1) : null;
					SuperCall callee = superCalls.get(i);

					if (!(callee instanceof CertainCall) && calleeEquality.match(callee)
							&& callerEquality.match(caller)) {
						// Change to CertainCall
						CertainCall certainCall = ArchModelUtils.createCertainCallElement(method);
						superCalls.set(i, certainCall);
						uBehaviorModified = true;
						modified = true;
					}
				}

				if (uBehaviorModified) {
					// Try to convert to certain Behavior.
					Behavior behavior = createCertainBehavior(uBehavior);
					if (behavior != null) {
						Connector connector = uConnector.getSuperInterface();
						addBehavior(connector, behavior);
						uBehaviorIt.remove();
					} else {
						// Check duplication.
						for (UncertainBehavior ub: uConnector.getU_behaviors()) {
							if (ub != uBehavior && ArchModelUtils.sameUncertainBehavior(ub, uBehavior)) {
								uBehaviorIt.remove();
							}
						}
					}
				}
			}
		}

		return modified;
	}

	@Override
	protected void operateCodeMethodInvocation(ExecutionEvent event, MethodInvocationInfo invInfo,
			ArchModel archModel) throws ExecutionException {
		String declClassName = invInfo.callerTypeName();
		String declName = invInfo.callerMethodName();
		String invClassName = invInfo.calleeTypeName();
		String invName = invInfo.calleeMethodName();
		Model model = archModel.getModel();

		boolean archModelModified = false;
		try {
			MethodEquality declEquality = null;
			if (invInfo.callerInheritingTypes != null) {
				String[] declClassNames = new String[invInfo.callerInheritingTypes.length];
				for (int i = 0; i < declClassNames.length; ++i) {
					declClassNames[i] = invInfo.callerInheritingTypes[i].getElementName();
				}
				declEquality = MethodEqualityUtils.createMethodEquality(declClassNames, declName);
			} else {
				declEquality = MethodEqualityUtils.createMethodEquality(declClassName, declName);
			}

			if (invInfo.calleeInheritingTypes != null) {
				for (IType type : invInfo.calleeInheritingTypes) {
					String inhClassName = type.getElementName();
					MethodEquality invEquality = MethodEqualityUtils.createMethodEquality(inhClassName, invName);
					archModelModified |= removeAsNecessary(inhClassName, declEquality, invEquality,
							invInfo.calleeBinding, model);
				}
			} else {
				MethodEquality invEquality = MethodEqualityUtils.createMethodEquality(invClassName, invName);
				archModelModified |= removeAsNecessary(invClassName, declEquality, invEquality,
						invInfo.calleeBinding, model);
			}
		} catch (ModelErrorException e) {
			System.out.println("RemoveUncertaintyAsNecessaryHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			saveArchModel(archModel);
		}
	}
	private boolean removeAsNecessary(String className, MethodEquality declEquality, MethodEquality invEquality,
			IMethodBinding calleeBinding, Model model) throws ModelErrorException {
		Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
		if (cInterface == null) {
			throw new ModelErrorException("Target interface does not exist: " + className);
		}
		boolean modified = false;

		Method method = getMethod(cInterface, calleeBinding);
		boolean methodCreated = false;
		if (method == null) {
			Method newMethod = ArchModelUtils.createMethodElement(calleeBinding);
			cInterface.getMethods().add(newMethod);
			method = newMethod;
			// Do not turn on modification flag
			// because new Method is needed only when behaviors are modified.
			methodCreated = true;
		}

		modified |= modifyUncertainCallToCertain(model, declEquality, invEquality, method);

		if (modified) {
			MethodEquality methodEquality = MethodEqualityUtils.createMethodEquality(method);
			modified |= removeUnusedUncertainMethod(model, className, methodEquality, true);
		}

		if (methodCreated && !modified) {
			// Discard unused Method.
			Interface interfaceToDetach = ArchModelUtils.getInterface(method, false);
			if (interfaceToDetach != null) {
				interfaceToDetach.getMethods().remove(method);
			} else {
				throw new ModelErrorException("Failed to discard unused Method: " + className);
			}
		}

		return modified;
	}

	private Method getMethod(Interface cInterface, IMethodBinding binding) {
		MethodEquality equality = MethodEqualityUtils.createMethodEquality(binding);
		return getMethod(cInterface, equality);
	}

	@Override
	protected void operateDiagramOperation(ExecutionEvent event,
			OperationInfo operationInfo, ArchModel archModel) throws ExecutionException {
		String className = operationInfo.typeName();
		String methodName = operationInfo.methodName();
		Model model = archModel.getModel();

		// Stop if Operation is certain.
		if (operationInfo.isCertain()) {
			System.out.println("RemoveUncertaintyAsNecessaryHandler: Operation is not uncertain: " + methodName);
			return;
		}

		// TODO: Assume that first method is to be necessary.
		String necessaryMethodName = operationInfo.methodNames().get(0);
		boolean archModelModified = false;
		try {
			Method method = ArchModelUtils.findMethodByName(operationInfo.cInterface, necessaryMethodName);
			if (method == null) {
				Method newMethod = null;
				Method baseMethod = findUncertainMethod(model, className, necessaryMethodName);
				if (baseMethod != null) {
					newMethod = ArchModelUtils.createMethodElement(baseMethod);
				} else {
					newMethod = ArchModelUtils.createMethodElement(necessaryMethodName);
				}
				operationInfo.cInterface.getMethods().add(newMethod);
				method = newMethod;
				// Do not turn on modification flag
				// because new Method is needed only when behaviors are modified.
			}

			MethodEquality calleeEquality = operationInfo.methodEquality();
			archModelModified |= modifyUncertainCallToCertain(model, MethodEqualityUtils.anyMethod, calleeEquality, method);

			MethodEquality methodEquality = MethodEqualityUtils.createMethodEquality(method);
			archModelModified |= removeUnusedUncertainMethod(model, className, methodEquality, true);

		} catch (ModelErrorException e) {
			System.out.println("RemoveUncertaintyAsNecessaryHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			boolean saved = saveArchModel(archModel);
			if (!saved) {
				System.out.println("RemoveUncertaintyAsNecessaryHandler: failed to save Archfile.");
			} else {
				boolean diagramModified =
						DiagramUtils.changeOperationToNecessary(operationInfo.operation, necessaryMethodName);
				if (!diagramModified) {
					System.out.println("RemoveUncertaintyAsNecessaryHandler: failed to modify Class diagram.");
				}
			}
		}
	}

	private Method findUncertainMethod(Model model, String className, String methodName) {
		MethodEquality equality = MethodEqualityUtils.createMethodEquality(methodName);
		for (UncertainInterface uInterface :
			ArchModelUtils.searchUncertainInterfaceBySuperName(model, className)) {
			for (OptMethod optMethod : uInterface.getOptmethods()) {
				Method method = optMethod.getMethod();
				if (equality.match(method)) {
					return method;
				}
			}
			for (AltMethod altMethod : uInterface.getAltmethods()) {
				for (Method method : altMethod.getMethods()) {
					if (equality.match(method)) {
						return method;
					}
				}
			}
		}
		return null;
	}

	@Override
	protected void operateDiagramMessage(ExecutionEvent event,
			MessageInfo messageInfo, ArchModel archModel) throws ExecutionException {
		String invClassName = messageInfo.calleeTypeName();
		String invName = messageInfo.calleeMethodName();
		Model model = archModel.getModel();

		// Stop if Message is certain.
		if (messageInfo.isCertain()) {
			System.out.println("RemoveUncertaintyAsNecessaryHandler: Message is not uncertain: " + invName);
			return;
		}

		// Necessary method is invName, which is selected by User.
		String necessaryMethodName = invName;
		boolean archModelModified = false;
		try {
			Method method = ArchModelUtils.findMethodByName(messageInfo.cInterface, necessaryMethodName);
			if (method == null) {
				Method newMethod = null;
				Method baseMethod = findUncertainMethod(model, invClassName, necessaryMethodName);
				if (baseMethod != null) {
					newMethod = ArchModelUtils.createMethodElement(baseMethod);
				} else {
					newMethod = ArchModelUtils.createMethodElement(necessaryMethodName);
				}
				messageInfo.cInterface.getMethods().add(newMethod);
				method = newMethod;
				// Do not turn on modification flag
				// because new Method is needed only when behaviors are modified.
			}

			MethodEquality invEquality = messageInfo.calleeMethodEquality();
			MethodEquality declEquality = messageInfo.callerMethodEquality();
			archModelModified |= modifyUncertainCallToCertain(model, declEquality, invEquality, method);
			if (archModelModified) {
				MethodEquality methodEquality = MethodEqualityUtils.createMethodEquality(method);
				archModelModified |= removeUnusedUncertainMethod(model, invClassName, methodEquality, true);
			}
		} catch (ModelErrorException e) {
			System.out.println("RemoveUncertaintyAsNecessaryHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			boolean saved = saveArchModel(archModel);
			if (!saved) {
				System.out.println("RemoveUncertaintyAsNecessaryHandler: failed to save Archfile.");
			} else {
				boolean diagramModified =
						DiagramUtils.changeMessageToNecessary(messageInfo.calleeMessage, necessaryMethodName);
				if (!diagramModified) {
					System.out.println("RemoveUncertaintyAsNecessaryHandler: failed to modify Sequence diagram.");
				}
			}
		}
	}
}
